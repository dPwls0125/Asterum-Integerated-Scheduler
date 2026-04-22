package com.vlast.scheduler.schedule.entity;

import com.vlast.scheduler.resource.entity.Resource;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "schedules")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private LocalDate scheduleDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    /**
     * NULL for one-time schedules.
     * Set when this schedule is an exception instance of a recurring series.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurrence_group_id")
    private RecurrenceGroup recurrenceGroup;

    /**
     * Soft-delete flag for recurring instance exceptions.
     * When true, this date is excluded from the recurring series.
     */
    @Builder.Default
    private boolean deleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ScheduleParticipant> participants = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** True if this is a one-time schedule (not part of any recurrence) */
    public boolean isOneTime() {
        return recurrenceGroup == null;
    }
}
