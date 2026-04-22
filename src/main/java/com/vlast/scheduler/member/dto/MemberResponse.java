package com.vlast.scheduler.member.dto;

import com.vlast.scheduler.member.entity.Member;
import com.vlast.scheduler.member.entity.MemberRole;

public record MemberResponse(
        Long id,
        String name,
        MemberRole role,
        String position,
        TeamResponse team
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getName(),
                member.getRole(),
                member.getPosition(),
                member.getTeam() != null ? TeamResponse.simple(member.getTeam()) : null
        );
    }

    public static MemberResponse simple(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getName(),
                member.getRole(),
                member.getPosition(),
                null
        );
    }
}
