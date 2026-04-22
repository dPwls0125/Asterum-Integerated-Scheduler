package com.vlast.scheduler.schedule.dto;

import java.time.LocalTime;
import java.util.List;

public record ScheduleUpdateRequest(
        String title,
        String description,
        LocalTime startTime,
        LocalTime endTime,
        List<Long> participantIds,
        List<Long> teamIds,
        Long resourceId
) {}
