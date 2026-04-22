package com.vlast.scheduler.member.service;

import com.vlast.scheduler.common.NotFoundException;
import com.vlast.scheduler.member.dto.MemberRequest;
import com.vlast.scheduler.member.dto.MemberResponse;
import com.vlast.scheduler.member.entity.Member;
import com.vlast.scheduler.member.entity.Team;
import com.vlast.scheduler.member.repository.MemberRepository;
import com.vlast.scheduler.member.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    public List<MemberResponse> findAll() {
        return memberRepository.findAll().stream().map(MemberResponse::from).toList();
    }

    public List<MemberResponse> findByTeamId(Long teamId) {
        return memberRepository.findByTeamId(teamId).stream().map(MemberResponse::from).toList();
    }

    @Transactional
    public MemberResponse create(MemberRequest request) {
        Team team = null;
        if (request.teamId() != null) {
            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new NotFoundException("팀을 찾을 수 없습니다: " + request.teamId()));
        }

        Member member = Member.builder()
                .name(request.name())
                .role(request.role())
                .position(request.position())
                .team(team)
                .build();
        return MemberResponse.from(memberRepository.save(member));
    }
}
