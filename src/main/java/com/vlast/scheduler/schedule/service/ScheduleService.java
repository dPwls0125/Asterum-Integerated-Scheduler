package com.vlast.scheduler.schedule.service;

import com.vlast.scheduler.common.NotFoundException;
import com.vlast.scheduler.member.entity.Member;
import com.vlast.scheduler.member.repository.MemberRepository;
import com.vlast.scheduler.resource.entity.Resource;
import com.vlast.scheduler.resource.repository.ResourceRepository;
import com.vlast.scheduler.schedule.dto.*;
import com.vlast.scheduler.schedule.entity.*;
import com.vlast.scheduler.schedule.repository.RecurrenceGroupRepository;
import com.vlast.scheduler.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final RecurrenceGroupRepository recurrenceGroupRepository;
    private final MemberRepository memberRepository;
    private final ResourceRepository resourceRepository;
    private final RecurrenceService recurrenceService;
    private final ResourceValidationService resourceValidationService;

    // ========== QUERY ==========

    /**
     * Get all schedules for a given month, including dynamically expanded recurring instances.
     */
    public List<ScheduleResponse> getMonthlySchedules(int year, int month) {
        LocalDate rangeStart = LocalDate.of(year, month, 1);
        LocalDate rangeEnd = rangeStart.withDayOfMonth(rangeStart.lengthOfMonth());

        List<ScheduleResponse> result = new ArrayList<>();

        // 1. One-time schedules
        List<Schedule> oneTimeSchedules = scheduleRepository.findOneTimeSchedulesInRange(rangeStart, rangeEnd);
        oneTimeSchedules.forEach(s -> result.add(ScheduleResponse.fromSchedule(s)));

        // 2. Recurring schedules (dynamic expansion)
        List<RecurrenceGroup> groups = recurrenceGroupRepository.findGroupsOverlappingRange(rangeStart, rangeEnd);
        for (RecurrenceGroup group : groups) {
            List<LocalDate> instanceDates = recurrenceService.expandInstances(group, rangeStart, rangeEnd);

            // Fetch exceptions for this group in range
            List<Schedule> exceptions = scheduleRepository.findExceptionsByGroupIdInRange(
                    group.getId(), rangeStart, rangeEnd);
            Map<LocalDate, Schedule> exceptionMap = exceptions.stream()
                    .collect(Collectors.toMap(Schedule::getScheduleDate, s -> s, (a, b) -> a));

            for (LocalDate date : instanceDates) {
                Schedule exception = exceptionMap.get(date);
                if (exception != null) {
                    if (!exception.isDeleted()) {
                        result.add(ScheduleResponse.fromSchedule(exception));
                    }
                    // else: deleted exception — skip this date
                } else {
                    result.add(ScheduleResponse.fromRecurrenceInstance(group, date));
                }
            }
        }

        result.sort(Comparator.comparing(ScheduleResponse::date)
                .thenComparing(ScheduleResponse::startTime));
        return result;
    }

    /**
     * Get detail of a single schedule by its ID.
     */
    public ScheduleResponse getScheduleDetail(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다: " + id));
        return ScheduleResponse.fromSchedule(schedule);
    }

    /**
     * Get detail of a virtual recurring instance.
     */
    public ScheduleResponse getRecurringInstanceDetail(Long groupId, LocalDate date) {
        RecurrenceGroup group = recurrenceGroupRepository.findByIdWithDetails(groupId)
                .orElseThrow(() -> new NotFoundException("반복 그룹을 찾을 수 없습니다: " + groupId));

        // Check if there's a materialized exception for this date
        List<Schedule> exceptions = scheduleRepository.findExceptionsByGroupIdInRange(groupId, date, date);
        if (!exceptions.isEmpty() && !exceptions.get(0).isDeleted()) {
            return ScheduleResponse.fromSchedule(exceptions.get(0));
        }

        return ScheduleResponse.fromRecurrenceInstance(group, date);
    }

    // ========== CREATE ==========

    @Transactional
    public ScheduleResponse createSchedule(ScheduleCreateRequest request) {
        // Acquire pessimistic lock on the resource FIRST to prevent race conditions.
        // This serializes concurrent booking attempts for the same resource.
        Resource resource = resolveResourceWithLock(request.resourceId());

        // Validate resource availability (within the lock scope)
        resourceValidationService.validateAvailability(
                request.resourceId(), request.startDate(), request.startTime(), request.endTime(), null);

        List<Member> participants = resolveParticipants(request.participantIds(), request.teamIds());

        if (request.recurrence() != null) {
            return createRecurringSchedule(request, resource, participants);
        } else {
            return createOneTimeSchedule(request, resource, participants);
        }
    }

    private ScheduleResponse createOneTimeSchedule(ScheduleCreateRequest request,
                                                     Resource resource, List<Member> participants) {
        Schedule schedule = Schedule.builder()
                .title(request.title())
                .description(request.description())
                .scheduleDate(request.startDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .resource(resource)
                .build();
        schedule = scheduleRepository.save(schedule);

        addParticipantsToSchedule(schedule, participants);
        return ScheduleResponse.fromSchedule(schedule);
    }

    private ScheduleResponse createRecurringSchedule(ScheduleCreateRequest request,
                                                      Resource resource, List<Member> participants) {
        RecurrenceRequest rec = request.recurrence();

        RecurrenceGroup group = RecurrenceGroup.builder()
                .title(request.title())
                .description(request.description())
                .recurrenceType(rec.type())
                .endType(rec.endType())
                .startDate(request.startDate())
                .endDate(rec.endDate())
                .endCount(rec.endCount())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .dayOfWeek(rec.type() == RecurrenceType.WEEKLY
                        ? request.startDate().getDayOfWeek().getValue() : null)
                .dayOfMonth(rec.type() == RecurrenceType.MONTHLY
                        ? request.startDate().getDayOfMonth() : null)
                .resource(resource)
                .build();
        group = recurrenceGroupRepository.save(group);

        addParticipantsToGroup(group, participants);
        return ScheduleResponse.fromRecurrenceInstance(group, request.startDate());
    }

    // ========== UPDATE ==========

    /**
     * Update a one-time (non-recurring) schedule.
     */
    @Transactional
    public ScheduleResponse updateOneTimeSchedule(Long scheduleId, ScheduleUpdateRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다: " + scheduleId));

        if (!schedule.isOneTime()) {
            throw new IllegalArgumentException("반복 일정은 /api/schedules/recurring 엔드포인트를 사용하세요.");
        }

        applyUpdateToSchedule(schedule, request);
        return ScheduleResponse.fromSchedule(schedule);
    }

    /**
     * Update a recurring schedule instance with scope control.
     */
    @Transactional
    public ScheduleResponse updateRecurringInstance(Long groupId, LocalDate instanceDate,
                                                     EditScope scope, ScheduleUpdateRequest request) {
        RecurrenceGroup group = recurrenceGroupRepository.findByIdWithDetails(groupId)
                .orElseThrow(() -> new NotFoundException("반복 그룹을 찾을 수 없습니다: " + groupId));

        return switch (scope) {
            case THIS -> updateThisInstance(group, instanceDate, request);
            case THIS_AND_FOLLOWING -> updateThisAndFollowing(group, instanceDate, request);
            case ALL -> updateAllInstances(group, request);
        };
    }

    private ScheduleResponse updateThisInstance(RecurrenceGroup group, LocalDate date,
                                                 ScheduleUpdateRequest request) {
        // Check if exception already exists
        List<Schedule> existing = scheduleRepository.findExceptionsByGroupIdInRange(
                group.getId(), date, date);

        Schedule exception;
        if (!existing.isEmpty()) {
            exception = existing.get(0);
        } else {
            // Materialize: create a new exception schedule
            exception = Schedule.builder()
                    .title(group.getTitle())
                    .description(group.getDescription())
                    .scheduleDate(date)
                    .startTime(group.getStartTime())
                    .endTime(group.getEndTime())
                    .recurrenceGroup(group)
                    .resource(group.getResource())
                    .build();
            exception = scheduleRepository.save(exception);

            // Copy default participants
            List<Member> defaultParticipants = group.getParticipants().stream()
                    .map(RecurrenceGroupParticipant::getMember).toList();
            addParticipantsToSchedule(exception, defaultParticipants);
        }

        applyUpdateToSchedule(exception, request);
        return ScheduleResponse.fromSchedule(exception);
    }

    private ScheduleResponse updateThisAndFollowing(RecurrenceGroup group, LocalDate fromDate,
                                                     ScheduleUpdateRequest request) {
        // Cut the original group to end before fromDate
        group.setEndType(EndType.UNTIL_DATE);
        group.setEndDate(fromDate.minusDays(1));
        recurrenceGroupRepository.save(group);

        // Delete exceptions from this date onwards
        List<Schedule> futureExceptions = scheduleRepository.findExceptionsByGroupId(group.getId())
                .stream().filter(s -> !s.getScheduleDate().isBefore(fromDate)).toList();
        scheduleRepository.deleteAll(futureExceptions);

        // Create a new group starting from fromDate
        RecurrenceGroup newGroup = RecurrenceGroup.builder()
                .title(request.title() != null ? request.title() : group.getTitle())
                .description(group.getDescription())
                .recurrenceType(group.getRecurrenceType())
                .endType(group.getEndType() == EndType.UNTIL_DATE ? EndType.NEVER : group.getEndType())
                .startDate(fromDate)
                .endDate(group.getEndDate())
                .endCount(group.getEndCount())
                .startTime(request.startTime() != null ? request.startTime() : group.getStartTime())
                .endTime(request.endTime() != null ? request.endTime() : group.getEndTime())
                .dayOfWeek(group.getDayOfWeek())
                .dayOfMonth(group.getDayOfMonth())
                .resource(resolveResource(request.resourceId() != null ? request.resourceId()
                        : (group.getResource() != null ? group.getResource().getId() : null)))
                .build();
        newGroup = recurrenceGroupRepository.save(newGroup);

        // Copy participants (or use new ones if specified)
        List<Member> participants = (request.participantIds() != null || request.teamIds() != null)
                ? resolveParticipants(request.participantIds(), request.teamIds())
                : group.getParticipants().stream().map(RecurrenceGroupParticipant::getMember).toList();
        addParticipantsToGroup(newGroup, participants);

        return ScheduleResponse.fromRecurrenceInstance(newGroup, fromDate);
    }

    private ScheduleResponse updateAllInstances(RecurrenceGroup group, ScheduleUpdateRequest request) {
        if (request.title() != null) group.setTitle(request.title());
        if (request.description() != null) group.setDescription(request.description());
        if (request.startTime() != null) group.setStartTime(request.startTime());
        if (request.endTime() != null) group.setEndTime(request.endTime());
        if (request.resourceId() != null) group.setResource(resolveResource(request.resourceId()));

        if (request.participantIds() != null || request.teamIds() != null) {
            group.getParticipants().clear();
            List<Member> participants = resolveParticipants(request.participantIds(), request.teamIds());
            addParticipantsToGroup(group, participants);
        }

        recurrenceGroupRepository.save(group);
        return ScheduleResponse.fromRecurrenceInstance(group, group.getStartDate());
    }

    // ========== DELETE ==========

    /**
     * Delete a one-time schedule.
     */
    @Transactional
    public void deleteOneTimeSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다: " + scheduleId));

        if (!schedule.isOneTime()) {
            throw new IllegalArgumentException("반복 일정은 /api/schedules/recurring 엔드포인트를 사용하세요.");
        }
        scheduleRepository.delete(schedule);
    }

    /**
     * Delete a recurring schedule instance with scope control.
     */
    @Transactional
    public void deleteRecurringInstance(Long groupId, LocalDate instanceDate, EditScope scope) {
        RecurrenceGroup group = recurrenceGroupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("반복 그룹을 찾을 수 없습니다: " + groupId));

        switch (scope) {
            case THIS -> deleteThisInstance(group, instanceDate);
            case THIS_AND_FOLLOWING -> deleteThisAndFollowing(group, instanceDate);
            case ALL -> deleteAllInstances(group);
        }
    }

    private void deleteThisInstance(RecurrenceGroup group, LocalDate date) {
        // Check if exception already exists
        List<Schedule> existing = scheduleRepository.findExceptionsByGroupIdInRange(
                group.getId(), date, date);

        if (!existing.isEmpty()) {
            Schedule exception = existing.get(0);
            exception.setDeleted(true);
            exception.setTitle(group.getTitle()); // keep title for reference
            scheduleRepository.save(exception);
        } else {
            // Create a "deleted" exception
            Schedule deletedException = Schedule.builder()
                    .title(group.getTitle())
                    .scheduleDate(date)
                    .startTime(group.getStartTime())
                    .endTime(group.getEndTime())
                    .recurrenceGroup(group)
                    .deleted(true)
                    .build();
            scheduleRepository.save(deletedException);
        }
    }

    private void deleteThisAndFollowing(RecurrenceGroup group, LocalDate fromDate) {
        if (fromDate.equals(group.getStartDate())) {
            deleteAllInstances(group);
        } else {
            group.setEndType(EndType.UNTIL_DATE);
            group.setEndDate(fromDate.minusDays(1));
            recurrenceGroupRepository.save(group);

            // Remove future exceptions
            List<Schedule> futureExceptions = scheduleRepository.findExceptionsByGroupId(group.getId())
                    .stream().filter(s -> !s.getScheduleDate().isBefore(fromDate)).toList();
            scheduleRepository.deleteAll(futureExceptions);
        }
    }

    private void deleteAllInstances(RecurrenceGroup group) {
        scheduleRepository.deleteByRecurrenceGroupId(group.getId());
        recurrenceGroupRepository.delete(group);
    }

    // ========== CONVERT ==========

    /**
     * Convert a one-time schedule into the first instance of a recurring series.
     */
    @Transactional
    public ScheduleResponse convertToRecurring(Long scheduleId, ConvertToRecurringRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다: " + scheduleId));

        if (!schedule.isOneTime()) {
            throw new IllegalArgumentException("이미 반복 일정입니다.");
        }

        RecurrenceRequest rec = request.recurrence();
        RecurrenceGroup group = RecurrenceGroup.builder()
                .title(schedule.getTitle())
                .description(schedule.getDescription())
                .recurrenceType(rec.type())
                .endType(rec.endType())
                .startDate(schedule.getScheduleDate())
                .endDate(rec.endDate())
                .endCount(rec.endCount())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .dayOfWeek(rec.type() == RecurrenceType.WEEKLY
                        ? schedule.getScheduleDate().getDayOfWeek().getValue() : null)
                .dayOfMonth(rec.type() == RecurrenceType.MONTHLY
                        ? schedule.getScheduleDate().getDayOfMonth() : null)
                .resource(schedule.getResource())
                .build();
        group = recurrenceGroupRepository.save(group);

        // Copy participants to group
        List<Member> participants = schedule.getParticipants().stream()
                .map(ScheduleParticipant::getMember).toList();
        addParticipantsToGroup(group, participants);

        // Delete the original one-time schedule (it will now be dynamically generated)
        scheduleRepository.delete(schedule);

        return ScheduleResponse.fromRecurrenceInstance(group, group.getStartDate());
    }

    // ========== HELPERS ==========

    private Resource resolveResource(Long resourceId) {
        if (resourceId == null) return null;
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("장소를 찾을 수 없습니다: " + resourceId));
    }

    /**
     * Resolve resource WITH pessimistic write lock.
     * Used during create/update to prevent race conditions:
     * Two concurrent transactions trying to book the same resource
     * will be serialized at the DB level by SELECT ... FOR UPDATE.
     */
    private Resource resolveResourceWithLock(Long resourceId) {
        if (resourceId == null) return null;
        return resourceRepository.findByIdWithLock(resourceId)
                .orElseThrow(() -> new NotFoundException("장소를 찾을 수 없습니다: " + resourceId));
    }

    /**
     * Resolve individual member IDs + team-based expansion into a deduplicated member list.
     */
    private List<Member> resolveParticipants(List<Long> memberIds, List<Long> teamIds) {
        Set<Long> allMemberIds = new LinkedHashSet<>();

        if (memberIds != null) {
            allMemberIds.addAll(memberIds);
        }
        if (teamIds != null) {
            for (Long teamId : teamIds) {
                List<Member> teamMembers = memberRepository.findByTeamId(teamId);
                teamMembers.forEach(m -> allMemberIds.add(m.getId()));
            }
        }

        if (allMemberIds.isEmpty()) return List.of();
        return memberRepository.findAllById(allMemberIds);
    }

    private void addParticipantsToSchedule(Schedule schedule, List<Member> members) {
        for (Member member : members) {
            ScheduleParticipant sp = ScheduleParticipant.builder()
                    .schedule(schedule)
                    .member(member)
                    .build();
            schedule.getParticipants().add(sp);
        }
        scheduleRepository.save(schedule);
    }

    private void addParticipantsToGroup(RecurrenceGroup group, List<Member> members) {
        for (Member member : members) {
            RecurrenceGroupParticipant rgp = RecurrenceGroupParticipant.builder()
                    .recurrenceGroup(group)
                    .member(member)
                    .build();
            group.getParticipants().add(rgp);
        }
        recurrenceGroupRepository.save(group);
    }

    private void applyUpdateToSchedule(Schedule schedule, ScheduleUpdateRequest request) {
        if (request.title() != null) schedule.setTitle(request.title());
        if (request.description() != null) schedule.setDescription(request.description());
        if (request.startTime() != null) schedule.setStartTime(request.startTime());
        if (request.endTime() != null) schedule.setEndTime(request.endTime());

        if (request.resourceId() != null) {
            // Lock resource row to prevent race conditions during update
            Resource resource = resolveResourceWithLock(request.resourceId());
            resourceValidationService.validateAvailability(
                    request.resourceId(), schedule.getScheduleDate(),
                    request.startTime() != null ? request.startTime() : schedule.getStartTime(),
                    request.endTime() != null ? request.endTime() : schedule.getEndTime(),
                    schedule.getId());
            schedule.setResource(resource);
        }

        if (request.participantIds() != null || request.teamIds() != null) {
            schedule.getParticipants().clear();
            List<Member> participants = resolveParticipants(request.participantIds(), request.teamIds());
            addParticipantsToSchedule(schedule, participants);
        }

        scheduleRepository.save(schedule);
    }
}
