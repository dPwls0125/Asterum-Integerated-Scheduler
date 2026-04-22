package com.vlast.scheduler.member.repository;

import com.vlast.scheduler.member.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}
