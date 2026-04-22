package com.vlast.scheduler.schedule.dto;

import com.vlast.scheduler.member.dto.MemberResponse;
import com.vlast.scheduler.resource.dto.ResourceResponse;
import com.vlast.scheduler.schedule.entity.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ScheduleResponse(
        Long id,
        String title,
        String description,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        boolean recurring,
        Long recurrenceGroupId,
        RecurrenceInfo recurrence,
        List<MemberResponse> participants,
        ResourceResponse resource
) {
    /** Build from a materialized Schedule entity */
    public static ScheduleResponse fromSchedule(Schedule schedule) {
        RecurrenceGroup rg = schedule.getRecurrenceGroup();
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getScheduleDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                rg != null,
                rg != null ? rg.getId() : null,
                rg != null ? RecurrenceInfo.from(rg) : null,
                schedule.getParticipants().stream()
                        .map(sp -> MemberResponse.simple(sp.getMember()))
                        .toList(),
                ResourceResponse.from(schedule.getResource())
        );
    }

    /** Build a virtual instance from a RecurrenceGroup for a specific date */
    public static ScheduleResponse fromRecurrenceInstance(RecurrenceGroup group, LocalDate instanceDate) {
        return new ScheduleResponse(
                null,
                group.getTitle(),
                group.getDescription(),
                instanceDate,
                group.getStartTime(),
                group.getEndTime(),
                true,
                group.getId(),
                RecurrenceInfo.from(group),
                group.getParticipants().stream()
                        .map(rgp -> MemberResponse.simple(rgp.getMember()))
                        .toList(),
                ResourceResponse.from(group.getResource())
        );
    }

    public record RecurrenceInfo(
            RecurrenceType type,
            EndType endType,
            LocalDate endDate,
            Integer endCount,
            LocalDate startDate
    ) {
        public static RecurrenceInfo from(RecurrenceGroup group) {
            return new RecurrenceInfo(
                    group.getRecurrenceType(),
                    group.getEndType(),
                    group.getEndDate(),
                    group.getEndCount(),
                    group.getStartDate()
            );
        }
    }
}
