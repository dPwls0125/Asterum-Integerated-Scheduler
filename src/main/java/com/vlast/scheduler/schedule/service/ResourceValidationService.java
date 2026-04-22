package com.vlast.scheduler.schedule.service;

import com.vlast.scheduler.common.ResourceConflictException;
import com.vlast.scheduler.schedule.entity.RecurrenceGroup;
import com.vlast.scheduler.schedule.entity.Schedule;
import com.vlast.scheduler.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Validates that a resource (venue) is available for booking.
 * Checks both materialized schedules and dynamically expanded recurring instances.
 */
@Service
@RequiredArgsConstructor
public class ResourceValidationService {

    private final ScheduleRepository scheduleRepository;
    private final RecurrenceService recurrenceService;
    private final com.vlast.scheduler.schedule.repository.RecurrenceGroupRepository recurrenceGroupRepository;

    /**
     * Validates that a resource is available at the given date/time.
     * @throws ResourceConflictException if the resource is already booked
     */
    public void validateAvailability(Long resourceId, LocalDate date,
                                     LocalTime startTime, LocalTime endTime,
                                     Long excludeScheduleId) {
        if (resourceId == null) return;

        // 1. Check materialized schedules (one-time + exceptions)
        List<Schedule> conflicts = scheduleRepository.findConflictingSchedules(
                resourceId, date, startTime, endTime, excludeScheduleId);

        if (!conflicts.isEmpty()) {
            throw new ResourceConflictException(
                    "해당 시간에 '" + conflicts.get(0).getResource().getName() + "'이(가) 이미 예약되어 있습니다: "
                    + conflicts.get(0).getTitle());
        }

        // 2. Check recurring schedules (dynamic expansion)
        List<RecurrenceGroup> groups = recurrenceGroupRepository
                .findGroupsOverlappingRange(date, date);
        for (RecurrenceGroup group : groups) {
            if (group.getResource() == null || !group.getResource().getId().equals(resourceId)) {
                continue;
            }
            List<LocalDate> instances = recurrenceService.expandInstances(group, date, date);
            if (!instances.isEmpty() && timesOverlap(group.getStartTime(), group.getEndTime(), startTime, endTime)) {
                throw new ResourceConflictException(
                        "해당 시간에 '" + group.getResource().getName() + "'이(가) 반복 일정으로 예약되어 있습니다: "
                        + group.getTitle());
            }
        }
    }

    private boolean timesOverlap(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }
}
