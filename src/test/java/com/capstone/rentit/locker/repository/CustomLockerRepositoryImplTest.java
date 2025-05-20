package com.capstone.rentit.locker.repository;

import com.capstone.rentit.config.QuerydslConfig;
import com.capstone.rentit.locker.domain.Device;
import com.capstone.rentit.locker.domain.Locker;
import com.capstone.rentit.locker.dto.LockerSearchForm;
import com.capstone.rentit.locker.repository.DeviceRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class})
class CustomLockerRepositoryImplTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private DeviceRepository deviceRepository;

    private CustomLockerRepositoryImpl repository;

    private Long deviceId1;
    private Long deviceId2;

    @BeforeEach
    void setUp() {
        // 1) Device 를 먼저 저장
        Device d1 = deviceRepository.save(
                Device.createDevice(new com.capstone.rentit.locker.dto.DeviceCreateForm("U1", "loc1"))
        );
        Device d2 = deviceRepository.save(
                Device.createDevice(new com.capstone.rentit.locker.dto.DeviceCreateForm("U2", "loc2"))
        );
        deviceId1 = d1.getDeviceId();
        deviceId2 = d2.getDeviceId();

        // 2) Locker 삽입
        persistLocker(deviceId1, 1L, true);
        persistLocker(deviceId1, 2L, false);
        persistLocker(deviceId2, 1L, true);

        // 3) Custom 리포지토리 초기화
        JPAQueryFactory qf = new JPAQueryFactory(em);
        repository = new CustomLockerRepositoryImpl(qf);
    }

    private void persistLocker(Long deviceId, Long lockerId, boolean available) {
        Locker locker = Locker.builder()
                .deviceId(deviceId)
                .lockerId(lockerId)
                .available(available)
                .activatedAt(LocalDateTime.now())
                .build();
        em.persist(locker);
    }

    @Test
    @DisplayName("필터 없이 조회하면 모든 사물함을 반환한다")
    void searchWithoutFilters() {
        var form = new LockerSearchForm(null, null);
        List<Locker> list = repository.search(form);

        assertThat(list)
                .hasSize(3)
                .extracting(Locker::getDeviceId, Locker::getLockerId, Locker::isAvailable)
                .containsExactlyInAnyOrder(
                        tuple(deviceId1, 1L, true),
                        tuple(deviceId1, 2L, false),
                        tuple(deviceId2, 1L, true)
                );
    }

    @Nested @DisplayName("deviceId 필터 적용")
    class DeviceIdFilter {
        @Test @DisplayName("deviceId=1 로 조회하면 해당 디바이스 사물함만 반환")
        void filterByDeviceId() {
            var form = new LockerSearchForm(deviceId1, null);
            List<Locker> list = repository.search(form);

            assertThat(list)
                    .hasSize(2)
                    .allMatch(l -> l.getDeviceId().equals(deviceId1));
        }
    }

    @Nested @DisplayName("available 필터 적용")
    class AvailableFilter {
        @Test @DisplayName("available=true 로 조회하면 사용 가능한 사물함만 반환")
        void filterByAvailableTrue() {
            var form = new LockerSearchForm(null, true);
            List<Locker> list = repository.search(form);

            assertThat(list)
                    .hasSize(2)
                    .allMatch(Locker::isAvailable);
        }

        @Test @DisplayName("available=false 로 조회하면 사용 불가능한 사물함만 반환")
        void filterByAvailableFalse() {
            var form = new LockerSearchForm(null, false);
            List<Locker> list = repository.search(form);

            assertThat(list)
                    .hasSize(1)
                    .allMatch(l -> !l.isAvailable());
        }
    }

    @Test
    @DisplayName("deviceId=1 && available=true 일 때 정확히 하나 반환")
    void filterByBoth() {
        var form = new LockerSearchForm(deviceId1, true);
        List<Locker> list = repository.search(form);

        assertThat(list)
                .hasSize(1)
                .first()
                .satisfies(l -> {
                    assertThat(l.getDeviceId()).isEqualTo(deviceId1);
                    assertThat(l.getLockerId()).isEqualTo(1L);
                    assertThat(l.isAvailable()).isTrue();
                });
    }
}
