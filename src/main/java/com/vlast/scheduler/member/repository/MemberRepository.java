package com.vlast.scheduler.member.repository;

import com.vlast.scheduler.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByTeamId(Long teamId);
}
