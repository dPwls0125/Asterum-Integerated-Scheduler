package com.vlast.scheduler.member.dto;

import com.vlast.scheduler.member.entity.Team;

import java.util.List;

public record TeamResponse(
        Long id,
        String name,
        String description,
        List<MemberResponse> members
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getMembers().stream().map(MemberResponse::simple).toList()
        );
    }

    public static TeamResponse simple(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getDescription(), null);
    }
}
