package com.vlast.scheduler.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ConvertToRecurringRequest(
        @NotNull(message = "반복 설정은 필수입니다")
        @Valid
        RecurrenceRequest recurrence
) {}
