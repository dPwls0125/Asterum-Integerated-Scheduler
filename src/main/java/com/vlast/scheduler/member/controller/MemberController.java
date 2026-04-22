package com.vlast.scheduler.member.controller;

import com.vlast.scheduler.member.dto.MemberRequest;
import com.vlast.scheduler.member.dto.MemberResponse;
import com.vlast.scheduler.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public List<MemberResponse> getAll() {
        return memberService.findAll();
    }

    @GetMapping(params = "teamId")
    public List<MemberResponse> getByTeam(@RequestParam Long teamId) {
        return memberService.findByTeamId(teamId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberRequest request) {
        return memberService.create(request);
    }
}
