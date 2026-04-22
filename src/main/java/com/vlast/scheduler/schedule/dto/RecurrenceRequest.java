package com.vlast.scheduler.schedule.dto;

import com.vlast.scheduler.schedule.entity.EndType;
import com.vlast.scheduler.schedule.entity.RecurrenceType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RecurrenceRequest(
        @NotNull(message = "반복 유형은 필수입니다")
        RecurrenceType type,
        @NotNull(message = "종료 조건은 필수입니다")
        EndType endType,
        LocalDate endDate,
        Integer endCount
) {}
