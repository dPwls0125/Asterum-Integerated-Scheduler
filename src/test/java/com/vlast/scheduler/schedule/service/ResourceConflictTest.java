package com.vlast.scheduler.schedule.service;

import com.vlast.scheduler.common.ResourceConflictException;
import com.vlast.scheduler.schedule.dto.ScheduleCreateRequest;
import com.vlast.scheduler.schedule.dto.ScheduleResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * 리소스(장소) 중복 예약 방지 테스트.
 *
 * - 동일 장소 + 겹치는 시간대 예약 시 충돌 감지
 * - Race Condition: 두 스레드가 동시에 같은 장소를 예약할 때 하나만 성공
 *
 * DataInitializer가 시드 데이터를 생성하므로 resourceId=1 (스튜디오 A)을 사용합니다.
 */
@SpringBootTest
class ResourceConflictTest {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private com.vlast.scheduler.schedule.repository.ScheduleRepository scheduleRepository;

    private static final Long STUDIO_A = 1L; // 스튜디오 A (시드 데이터)

    @AfterEach
    void tearDown() {
        // 동시성 테스트의 자식 스레드가 생성한 데이터는 메인 스레드의 @Transactional로
        // 롤백할 수 없으므로, 매 테스트가 끝난 후 명시적으로 스케줄 데이터를 모두 지웁니다.
        scheduleRepository.deleteAllInBatch();
    }

    // ====================================================================
    // 1. 기본 충돌 감지 테스트
    // ====================================================================

    @Test
    @DisplayName("동일 장소 + 겹치는 시간 → ResourceConflictException 발생")
    void 동일_장소_겹치는_시간_충돌() {
        // given: 스튜디오 A에 10:00~12:00 일정 등록
        scheduleService.createSchedule(request(
                "MV 촬영", LocalDate.of(2026, 7, 1),
                LocalTime.of(10, 0), LocalTime.of(12, 0), STUDIO_A));

        // when & then: 같은 날 같은 장소 11:00~13:00 예약 → 시간이 겹치므로 충돌
        assertThatThrownBy(() -> scheduleService.createSchedule(request(
                "인터뷰 촬영", LocalDate.of(2026, 7, 1),
                LocalTime.of(11, 0), LocalTime.of(13, 0), STUDIO_A))).isInstanceOf(ResourceConflictException.class);
    }

    @Test
    @DisplayName("동일 장소 + 겹치지 않는 시간 → 정상 등록")
    void 동일_장소_다른_시간_정상() {
        // given: 스튜디오 A에 09:00~10:00
        scheduleService.createSchedule(request(
                "오전 촬영", LocalDate.of(2026, 7, 2),
                LocalTime.of(9, 0), LocalTime.of(10, 0), STUDIO_A));

        // when: 같은 날 같은 장소 10:00~11:00 (바로 이어서, 겹치지 않음)
        ScheduleResponse result = scheduleService.createSchedule(request(
                "오전 회의", LocalDate.of(2026, 7, 2),
                LocalTime.of(10, 0), LocalTime.of(11, 0), STUDIO_A));

        // then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("오전 회의");
    }

    @Test
    @DisplayName("다른 장소 + 같은 시간 → 정상 등록")
    void 다른_장소_같은_시간_정상() {
        Long studioB = 2L; // 스튜디오 B

        // given: 스튜디오 A에 14:00~16:00
        scheduleService.createSchedule(request(
                "촬영 A", LocalDate.of(2026, 7, 3),
                LocalTime.of(14, 0), LocalTime.of(16, 0), STUDIO_A));

        // when: 스튜디오 B에 같은 시간
        ScheduleResponse result = scheduleService.createSchedule(request(
                "촬영 B", LocalDate.of(2026, 7, 3),
                LocalTime.of(14, 0), LocalTime.of(16, 0), studioB));

        // then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("촬영 B");
    }

    @Test
    @DisplayName("장소 미지정 일정 → 충돌 검사 없이 정상 등록")
    void 장소_미지정_충돌없음() {
        // given: 스튜디오 A에 08:00~09:00 등록
        scheduleService.createSchedule(request(
                "스튜디오 일정", LocalDate.of(2026, 7, 4),
                LocalTime.of(8, 0), LocalTime.of(9, 0), STUDIO_A));

        // when: 같은 시간이지만 장소 미지정
        ScheduleResponse result = scheduleService.createSchedule(request(
                "온라인 미팅", LocalDate.of(2026, 7, 4),
                LocalTime.of(8, 0), LocalTime.of(9, 0), null));

        // then
        assertThat(result).isNotNull();
    }

    // ====================================================================
    // 2. Race Condition 동시성 테스트
    // ====================================================================

    @Test
    @DisplayName("Race Condition: 2개 스레드 동시 예약 → 비관적 락으로 1개만 성공")
    void 동시_예약_비관적_락_테스트() throws InterruptedException {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount); // 두 스레드가 모두 준비될 때까지 대기
        CountDownLatch startLatch = new CountDownLatch(1); // 동시에 시작하기 위한 신호
        CountDownLatch doneLatch = new CountDownLatch(threadCount); // 두 스레드가 모두 끝날 때까지 대기

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicReference<String> successTitle = new AtomicReference<>();

        // 같은 날, 같은 시간, 같은 장소를 동시에 예약
        LocalDate date = LocalDate.of(2026, 8, 15);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(12, 0);

        for (int i = 0; i < threadCount; i++) {
            final String title = "동시 예약 " + (i + 1);
            executor.submit(() -> {
                try {
                    readyLatch.countDown(); // 준비 완료 알림
                    startLatch.await(); // 시작 신호 대기 (동시 출발)

                    ScheduleResponse response = scheduleService.createSchedule(
                            request(title, date, start, end, STUDIO_A));
                    successCount.incrementAndGet();
                    successTitle.set(response.title());
                } catch (ResourceConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // 비관적 락 타임아웃 등 예외도 충돌로 처리
                    conflictCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(); // 두 스레드 모두 준비될 때까지 대기
        startLatch.countDown(); // 동시 출발 신호!
        doneLatch.await(); // 결과 대기

        executor.shutdown();

        // then: 정확히 1개만 성공, 1개는 충돌
        assertThat(successCount.get())
                .as("동시 예약 중 정확히 1개만 성공해야 함")
                .isEqualTo(1);
        assertThat(conflictCount.get())
                .as("동시 예약 중 정확히 1개는 충돌해야 함")
                .isEqualTo(1);
        assertThat(successTitle.get()).startsWith("동시 예약");

        System.out.println("✅ Race Condition 테스트 통과: 성공=" + successCount.get()
                + ", 충돌=" + conflictCount.get()
                + ", 성공 일정='" + successTitle.get() + "'");
    }

    @Test
    @DisplayName("Race Condition: 10개 스레드 동시 예약 → 1개만 성공")
    void 대량_동시_예약_비관적_락_테스트() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        LocalDate date = LocalDate.of(2026, 9, 1);
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(16, 0);

        for (int i = 0; i < threadCount; i++) {
            final String title = "대량 동시 예약 " + (i + 1);
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    scheduleService.createSchedule(
                            request(title, date, start, end, STUDIO_A));
                    successCount.incrementAndGet();
                } catch (ResourceConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();

        executor.shutdown();

        assertThat(successCount.get())
                .as("10개 동시 예약 중 정확히 1개만 성공해야 함")
                .isEqualTo(1);
        assertThat(conflictCount.get())
                .as("나머지 9개는 모두 충돌해야 함")
                .isEqualTo(threadCount - 1);

        System.out.println("대량 동시성 테스트 통과: 성공=" + successCount.get()
                + ", 충돌=" + conflictCount.get());
    }

    // ====================================================================
    // Helper
    // ====================================================================

    private ScheduleCreateRequest request(String title, LocalDate date,
            LocalTime start, LocalTime end,
            Long resourceId) {
        return new ScheduleCreateRequest(
                title, null, date, start, end,
                List.of(), null, resourceId, null);
    }
}
