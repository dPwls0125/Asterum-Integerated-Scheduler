package com.vlast.scheduler.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ScheduleCreateRequest(
        @NotBlank(message = "제목은 필수입니다")
        String title,
        String description,
        @NotNull(message = "날짜는 필수입니다")
        LocalDate startDate,
        @NotNull(message = "시작 시간은 필수입니다")
        LocalTime startTime,
        @NotNull(message = "종료 시간은 필수입니다")
        LocalTime endTime,
        List<Long> participantIds,
        List<Long> teamIds,
        Long resourceId,
        @Valid
        RecurrenceRequest recurrence
) {}
