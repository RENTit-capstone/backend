package com.capstone.rentit.locker.service;

import com.capstone.rentit.locker.domain.Locker;
import com.capstone.rentit.locker.dto.LockerCreateForm;
import com.capstone.rentit.locker.dto.LockerDto;
import com.capstone.rentit.locker.dto.LockerSearchForm;
import com.capstone.rentit.locker.repository.LockerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockerServiceTest {

    @Mock
    private LockerRepository lockerRepository;

    @InjectMocks
    private LockerService lockerService;

    /**
     * protected no-args 생성자를 가진 Locker 인스턴스를 리플렉션으로 생성하고
     * 필드값을 직접 세팅해 돌려줍니다.
     */
    private Locker createLockerEntity(Long id, String university, Boolean available) throws Exception {
        Constructor<Locker> ctor = Locker.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        Locker locker = ctor.newInstance();
        ReflectionTestUtils.setField(locker, "lockerId", id);
        ReflectionTestUtils.setField(locker, "university", university);
        ReflectionTestUtils.setField(locker, "available", available);
        return locker;
    }

    @Test
    @DisplayName("registerLocker: 정상 저장 시 repository.save 호출 후 ID 반환")
    void registerLocker_success() {
        // given
        LockerCreateForm form = LockerCreateForm.builder()
                .university("Seoul Univ").locationDescription("building 1, floor 3").build();
        Locker saved = mock(Locker.class);
        when(saved.getLockerId()).thenReturn(42L);
        when(lockerRepository.save(any(Locker.class))).thenReturn(saved);

        // when
        Long result = lockerService.registerLocker(form);

        // then
        assertThat(result).isEqualTo(42L);
        ArgumentCaptor<Locker> captor = ArgumentCaptor.forClass(Locker.class);
        verify(lockerRepository).save(captor.capture());
        Locker toSave = captor.getValue();
        assertThat(toSave.getUniversity()).isEqualTo("Seoul Univ");
        assertThat(toSave.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("getLocker: 존재하는 ID 조회 시 DTO 로 매핑되어 반환")
    void getLocker_exists() throws Exception {
        // given
        Locker entity = createLockerEntity(7L, "Test Univ", false);
        when(lockerRepository.findById(7L)).thenReturn(Optional.of(entity));

        // when
        LockerDto dto = lockerService.getLocker(7L);

        // then
        assertThat(dto.getLockerId()).isEqualTo(7L);
        assertThat(dto.getUniversity()).isEqualTo("Test Univ");
        assertThat(dto.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("getLocker: 없는 ID 조회 시 IllegalArgumentException 예외 발생")
    void getLocker_notFound_throws() {
        // given
        when(lockerRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> lockerService.getLocker(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사물함이 존재하지 않습니다. id=99");
    }

    @Test
    @DisplayName("searchLockers: 전달된 SearchForm 으로 조회한 엔티티들을 DTO 리스트로 변환")
    void searchLockers_mapsEntitiesToDto() throws Exception {
        // given
        Locker a = createLockerEntity(1L, "U1", true);
        Locker b = createLockerEntity(2L, "U2", false);
        LockerSearchForm form = new LockerSearchForm("ignore", null);

        when(lockerRepository.search(form)).thenReturn(List.of(a, b));

        // when
        List<LockerDto> dtos = lockerService.searchLockers(form);

        // then
        assertThat(dtos)
                .extracting(LockerDto::getLockerId)
                .containsExactly(1L, 2L);
        assertThat(dtos)
                .extracting(LockerDto::getUniversity)
                .containsExactly("U1", "U2");
    }

    @Test
    @DisplayName("findAvailableLockers: university + available=true 로 조회 후 DTO 변환")
    void findAvailableLockers_filtersAvailableTrue() throws Exception {
        // given
        Locker available = createLockerEntity(3L, "Univ X", true);
        when(lockerRepository.search(any(LockerSearchForm.class)))
                .thenReturn(List.of(available));

        // when
        List<LockerDto> dtos = lockerService.findAvailableLockers("Univ X");

        // then
        assertThat(dtos).hasSize(1);
        LockerDto dto = dtos.get(0);
        assertThat(dto.getLockerId()).isEqualTo(3L);
        assertThat(dto.getUniversity()).isEqualTo("Univ X");
        assertThat(dto.isAvailable()).isTrue();

        // 내부에서 생성한 SearchForm 인자 검증
        ArgumentCaptor<LockerSearchForm> captor = ArgumentCaptor.forClass(LockerSearchForm.class);
        verify(lockerRepository).search(captor.capture());
        LockerSearchForm passed = captor.getValue();
        assertThat(passed.getUniversity()).isEqualTo("Univ X");
        assertThat(passed.getAvailable()).isTrue();
    }
}