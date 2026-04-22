package com.vlast.scheduler.schedule.entity;

import com.vlast.scheduler.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recurrence_group_participants")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecurrenceGroupParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurrence_group_id", nullable = false)
    private RecurrenceGroup recurrenceGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
