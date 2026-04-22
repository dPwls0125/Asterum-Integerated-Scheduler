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
@Table(name = "recurrence_groups")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecurrenceGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceType recurrenceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EndType endType;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    private Integer endCount;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    /** WEEKLY: 1=MON ~ 7=SUN (ISO DayOfWeek) */
    private Integer dayOfWeek;

    /** MONTHLY: 1~31 */
    private Integer dayOfMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @OneToMany(mappedBy = "recurrenceGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecurrenceGroupParticipant> participants = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
