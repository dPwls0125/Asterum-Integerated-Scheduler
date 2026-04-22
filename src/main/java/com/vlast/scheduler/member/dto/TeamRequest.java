package com.vlast.scheduler.member.dto;

import jakarta.validation.constraints.NotBlank;

public record TeamRequest(
        @NotBlank(message = "팀 이름은 필수입니다")
        String name,
        String description
) {}
