package com.vlast.scheduler.member.dto;

import com.vlast.scheduler.member.entity.MemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemberRequest(
        @NotBlank(message = "이름은 필수입니다")
        String name,
        @NotNull(message = "역할은 필수입니다")
        MemberRole role,
        String position,
        Long teamId
) {}
