package com.vlast.scheduler.schedule.repository;

import com.vlast.scheduler.schedule.entity.RecurrenceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurrenceGroupRepository extends JpaRepository<RecurrenceGroup, Long> {

    /** Find recurrence groups that potentially overlap with a date range */
    @Query("SELECT rg FROM RecurrenceGroup rg LEFT JOIN FETCH rg.participants p " +
           "LEFT JOIN FETCH p.member LEFT JOIN FETCH rg.resource " +
           "WHERE rg.startDate <= :rangeEnd " +
           "AND (rg.endType = 'NEVER' OR rg.endDate IS NULL OR rg.endDate >= :rangeStart)")
    List<RecurrenceGroup> findGroupsOverlappingRange(LocalDate rangeStart, LocalDate rangeEnd);

    @Query("SELECT rg FROM RecurrenceGroup rg LEFT JOIN FETCH rg.participants p " +
           "LEFT JOIN FETCH p.member LEFT JOIN FETCH rg.resource WHERE rg.id = :id")
    Optional<RecurrenceGroup> findByIdWithDetails(Long id);
}
