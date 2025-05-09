package com.capstone.rentit.locker.service;

import com.capstone.rentit.locker.domain.Device;
import com.capstone.rentit.locker.domain.Locker;
import com.capstone.rentit.locker.dto.*;
import com.capstone.rentit.locker.exception.LockerNotFoundException;
import com.capstone.rentit.locker.repository.DeviceRepository;
import com.capstone.rentit.locker.repository.LockerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockerServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private LockerRepository lockerRepository;

    private LockerService lockerService;

    @BeforeEach
    void setUp() {
        lockerService = new LockerService(deviceRepository, lockerRepository);
    }

    @Test
    @DisplayName("registerDevice: 정상적으로 디바이스 저장 후 ID 반환")
    void registerDevice_success() {
        // given
        DeviceCreateForm form = new DeviceCreateForm("Uni1", "Desc1");
        Device saved = Device.builder()
                .deviceId(100L)
                .university(form.getUniversity())
                .locationDescription(form.getLocationDescription())
                .build();
        when(deviceRepository.save(any(Device.class))).thenReturn(saved);

        // when
        Long resultId = lockerService.registerDevice(form);

        // then
        assertThat(resultId).isEqualTo(100L);
        ArgumentCaptor<Device> cap = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepository).save(cap.capture());
        Device arg = cap.getValue();
        assertThat(arg.getUniversity()).isEqualTo("Uni1");
        assertThat(arg.getLocationDescription()).isEqualTo("Desc1");
    }

    @Nested
    @DisplayName("registerLocker: 다음 lockerId 자동증가")
    class RegisterLocker {
        @Test
        @DisplayName("최초 등록 시 nextId=1 반환")
        void firstLocker() {
            // given
            LockerCreateForm form = new LockerCreateForm(10L);
            when(lockerRepository.findMaxLockerIdByDeviceId(10L))
                    .thenReturn(Optional.empty());
            // echo back saved entity
            when(lockerRepository.save(any(Locker.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // when
            Long id = lockerService.registerLocker(form);

            // then
            assertThat(id).isEqualTo(1L);
            ArgumentCaptor<Locker> cap = ArgumentCaptor.forClass(Locker.class);
            verify(lockerRepository).save(cap.capture());
            Locker arg = cap.getValue();
            assertThat(arg.getDeviceId()).isEqualTo(10L);
            assertThat(arg.getLockerId()).isEqualTo(1L);
            assertThat(arg.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("기존 max=5 일 때 nextId=6 반환")
        void subsequentLocker() {
            // given
            LockerCreateForm form = new LockerCreateForm(20L);
            when(lockerRepository.findMaxLockerIdByDeviceId(20L))
                    .thenReturn(Optional.of(5L));
            when(lockerRepository.save(any(Locker.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // when
            Long id = lockerService.registerLocker(form);

            // then
            assertThat(id).isEqualTo(6L);
            ArgumentCaptor<Locker> cap = ArgumentCaptor.forClass(Locker.class);
            verify(lockerRepository).save(cap.capture());
            assertThat(cap.getValue().getLockerId()).isEqualTo(6L);
        }
    }

    @Nested
    @DisplayName("searchDevicesByUniversity: 대학명 조회")
    class SearchDevices {
        @Test
        @DisplayName("조회 결과 매핑 테스트")
        void searchDevicesByUniversity() {
            // given
            Device d1 = Device.builder().deviceId(1L)
                    .university("U1").locationDescription("L1").build();
            Device d2 = Device.builder().deviceId(2L)
                    .university("U1").locationDescription("L2").build();
            when(deviceRepository.findByUniversity("U1"))
                    .thenReturn(List.of(d1, d2));

            // when
            List<DeviceResponse> list = lockerService
                    .searchDevicesByUniversity(new DeviceSearchForm("U1"));

            // then
            assertThat(list).hasSize(2)
                    .extracting(DeviceResponse::getDeviceId,
                            DeviceResponse::getUniversity,
                            DeviceResponse::getLocationDescription)
                    .containsExactlyInAnyOrder(
                            tuple(1L, "U1", "L1"),
                            tuple(2L, "U1", "L2")
                    );
        }

        @Test
        @DisplayName("조회 결과가 없을 때 빈 리스트 반환")
        void emptyResult() {
            when(deviceRepository.findByUniversity("X"))
                    .thenReturn(List.of());
            assertThat(lockerService.searchDevicesByUniversity(
                    new DeviceSearchForm("X"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchLockers: 복합 검색 테스트")
    class SearchLockers {
        @Test
        @DisplayName("검색 결과 매핑 테스트")
        void searchLockers() {
            // given
            Locker l1 = Locker.builder()
                    .deviceId(3L).lockerId(1L).available(true)
                    .device(Device.builder().deviceId(3L).build())
                    .activatedAt(LocalDateTime.now()).build();
            Locker l2 = Locker.builder()
                    .deviceId(3L).lockerId(2L).available(false)
                    .device(Device.builder().deviceId(3L).build())
                    .activatedAt(LocalDateTime.now()).build();
            LockerSearchForm form = new LockerSearchForm(3L, null);
            when(lockerRepository.search(form)).thenReturn(List.of(l1, l2));

            // when
            List<LockerResponse> list = lockerService.searchLockers(form);

            // then
            assertThat(list).hasSize(2)
                    .extracting(LockerResponse::getDeviceId,
                            LockerResponse::getLockerId,
                            LockerResponse::isAvailable)
                    .contains(tuple(3L, 1L, true),
                            tuple(3L, 2L, false));
        }
    }

    @Nested
    @DisplayName("findAvailableLockers: 사용 가능 사물함 조회")
    class FindAvailable {
        @Test
        @DisplayName("사용 가능 칸만 LockerBriefResponse로 매핑")
        void findAvailableLockers() {
            // given
            Locker l1 = Locker.builder().deviceId(5L).lockerId(1L)
                    .available(true).activatedAt(LocalDateTime.now()).build();
            Locker l2 = Locker.builder().deviceId(5L).lockerId(2L)
                    .available(true).activatedAt(LocalDateTime.now()).build();
            when(lockerRepository.search(any())).thenReturn(List.of(l1, l2));

            // when
            List<LockerBriefResponse> list = lockerService.findAvailableLockers(5L);

            // then
            assertThat(list).hasSize(2)
                    .allMatch(br -> br.deviceId().equals(5L) && br.available());
        }
    }
}
