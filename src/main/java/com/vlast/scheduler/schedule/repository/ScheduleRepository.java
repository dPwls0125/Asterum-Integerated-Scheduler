package com.vlast.scheduler.schedule.repository;

import com.vlast.scheduler.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /** One-time schedules in date range (no recurrence group) */
    @Query("SELECT s FROM Schedule s LEFT JOIN FETCH s.participants p LEFT JOIN FETCH p.member " +
           "LEFT JOIN FETCH s.resource WHERE s.recurrenceGroup IS NULL " +
           "AND s.scheduleDate >= :startDate AND s.scheduleDate <= :endDate")
    List<Schedule> findOneTimeSchedulesInRange(LocalDate startDate, LocalDate endDate);

    /** Exception instances for a recurrence group */
    @Query("SELECT s FROM Schedule s LEFT JOIN FETCH s.participants p LEFT JOIN FETCH p.member " +
           "LEFT JOIN FETCH s.resource WHERE s.recurrenceGroup.id = :groupId")
    List<Schedule> findExceptionsByGroupId(Long groupId);

    /** Exception instances for a recurrence group within date range */
    @Query("SELECT s FROM Schedule s LEFT JOIN FETCH s.participants p LEFT JOIN FETCH p.member " +
           "LEFT JOIN FETCH s.resource WHERE s.recurrenceGroup.id = :groupId " +
           "AND s.scheduleDate >= :startDate AND s.scheduleDate <= :endDate")
    List<Schedule> findExceptionsByGroupIdInRange(Long groupId, LocalDate startDate, LocalDate endDate);

    /** Check resource conflicts for a given date and time range */
    @Query("SELECT s FROM Schedule s WHERE s.resource.id = :resourceId " +
           "AND s.scheduleDate = :date AND s.deleted = false " +
           "AND s.startTime < :endTime AND s.endTime > :startTime " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId)")
    List<Schedule> findConflictingSchedules(Long resourceId, LocalDate date,
                                            LocalTime startTime, LocalTime endTime, Long excludeId);

    /** Delete all exception instances for a recurrence group */
    void deleteByRecurrenceGroupId(Long groupId);

    /** Delete exception instances from a date onwards */
    @Query("DELETE FROM Schedule s WHERE s.recurrenceGroup.id = :groupId AND s.scheduleDate >= :fromDate")
    void deleteExceptionsFromDate(Long groupId, LocalDate fromDate);
}
