package com.vlast.scheduler.init;

import com.vlast.scheduler.member.entity.Member;
import com.vlast.scheduler.member.entity.MemberRole;
import com.vlast.scheduler.member.entity.Team;
import com.vlast.scheduler.member.repository.MemberRepository;
import com.vlast.scheduler.member.repository.TeamRepository;
import com.vlast.scheduler.resource.entity.Resource;
import com.vlast.scheduler.resource.entity.ResourceType;
import com.vlast.scheduler.resource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final ResourceRepository resourceRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (teamRepository.count() > 0) {
            log.info("시드 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("시드 데이터 초기화 시작...");

        // Teams
        Team vocalTeam = teamRepository.save(Team.builder().name("보컬팀").description("보컬 트레이닝 및 녹음 담당").build());
        Team danceTeam = teamRepository.save(Team.builder().name("안무팀").description("안무 구성 및 트레이닝 담당").build());
        Team videoTeam = teamRepository.save(Team.builder().name("영상팀").description("MV 및 콘텐츠 촬영 담당").build());
        Team styleTeam = teamRepository.save(Team.builder().name("스타일링팀").description("의상 및 메이크업 담당").build());
        Team mgmtTeam = teamRepository.save(Team.builder().name("매니지먼트팀").description("스케줄 및 대외 업무 담당").build());

        // Artists
        memberRepository
                .save(Member.builder().name("예준").role(MemberRole.ARTIST).position("보컬").team(vocalTeam).build());
        memberRepository
                .save(Member.builder().name("밤비").role(MemberRole.ARTIST).position("댄서").team(vocalTeam).build());
        memberRepository
                .save(Member.builder().name("하민").role(MemberRole.ARTIST).position("래퍼").team(danceTeam).build());
        memberRepository
                .save(Member.builder().name("은호").role(MemberRole.ARTIST).position("래퍼").team(danceTeam).build());
        memberRepository
                .save(Member.builder().name("노아").role(MemberRole.ARTIST).position("보컬").team(danceTeam).build());

        // Staff
        memberRepository
                .save(Member.builder().name("김PD").role(MemberRole.STAFF).position("총괄 프로듀서").team(videoTeam).build());
        memberRepository
                .save(Member.builder().name("이감독").role(MemberRole.STAFF).position("영상 감독").team(videoTeam).build());
        memberRepository
                .save(Member.builder().name("박안무").role(MemberRole.STAFF).position("안무 감독").team(danceTeam).build());
        memberRepository.save(
                Member.builder().name("최스타일").role(MemberRole.STAFF).position("수석 스타일리스트").team(styleTeam).build());
        memberRepository
                .save(Member.builder().name("정매니저").role(MemberRole.STAFF).position("매니저").team(mgmtTeam).build());
        memberRepository
                .save(Member.builder().name("한기술").role(MemberRole.STAFF).position("기술 감독").team(videoTeam).build());
        memberRepository
                .save(Member.builder().name("오음악").role(MemberRole.STAFF).position("음악 감독").team(vocalTeam).build());

        // Resources (venues)
        resourceRepository.save(Resource.builder().name("스튜디오 A").type(ResourceType.STUDIO).build());
        resourceRepository.save(Resource.builder().name("스튜디오 B").type(ResourceType.STUDIO).build());
        resourceRepository.save(Resource.builder().name("리허설 홀").type(ResourceType.REHEARSAL_ROOM).build());
        resourceRepository.save(Resource.builder().name("녹음실").type(ResourceType.RECORDING_STUDIO).build());
        resourceRepository.save(Resource.builder().name("야외 촬영지").type(ResourceType.OUTDOOR).build());

        log.info("시드 데이터 초기화 완료: 팀 {}개, 멤버 {}개, 장소 {}개",
                teamRepository.count(), memberRepository.count(), resourceRepository.count());
    }
}
