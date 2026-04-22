package com.vlast.scheduler.member.service;

import com.vlast.scheduler.common.NotFoundException;
import com.vlast.scheduler.member.dto.TeamRequest;
import com.vlast.scheduler.member.dto.TeamResponse;
import com.vlast.scheduler.member.entity.Team;
import com.vlast.scheduler.member.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;

    public List<TeamResponse> findAll() {
        return teamRepository.findAll().stream().map(TeamResponse::from).toList();
    }

    public TeamResponse findById(Long id) {
        return TeamResponse.from(getTeamOrThrow(id));
    }

    @Transactional
    public TeamResponse create(TeamRequest request) {
        Team team = Team.builder()
                .name(request.name())
                .description(request.description())
                .build();
        return TeamResponse.from(teamRepository.save(team));
    }

    public Team getTeamOrThrow(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("팀을 찾을 수 없습니다: " + id));
    }
}
