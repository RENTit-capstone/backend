package com.capstone.rentit.rental.service;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.dto.RentalDto;
import com.capstone.rentit.rental.dto.RentalRequestForm;
import com.capstone.rentit.rental.repository.RentalRepository;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private RentalService rentalService;

    private RentalRequestForm baseForm;

    @BeforeEach
    void setUp() {
        baseForm = new RentalRequestForm();
        baseForm.setItemId(100L);
        baseForm.setOwnerId(10L);
        baseForm.setRenterId(20L);
        baseForm.setStartDate(LocalDateTime.now().plusDays(1));
        baseForm.setDueDate(LocalDateTime.now().plusDays(7));
    }

    @Test
    @DisplayName("requestRental: 요청한 필드대로 Rental 엔티티가 저장되고 ID 반환")
    void requestRental_savesCorrectEntity() {
        // given
        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        Rental saved = Rental.builder()
                .rentalId(1L)
                .itemId(100L)
                .ownerId(10L)
                .renterId(20L)
                .status(RentalStatusEnum.REQUESTED)
                .startDate(baseForm.getStartDate())
                .dueDate(baseForm.getDueDate())
                .requestDate(LocalDateTime.now())
                .build();
        given(rentalRepository.save(any(Rental.class))).willReturn(saved);

        // when
        Long resultId = rentalService.requestRental(baseForm);

        // then
        assertThat(resultId).isEqualTo(1L);
        verify(rentalRepository).save(captor.capture());
        Rental toSave = captor.getValue();
        assertThat(toSave.getItemId()).isEqualTo(100L);
        assertThat(toSave.getOwnerId()).isEqualTo(10L);
        assertThat(toSave.getRenterId()).isEqualTo(20L);
        assertThat(toSave.getStatus()).isEqualTo(RentalStatusEnum.REQUESTED);
        assertThat(toSave.getStartDate()).isEqualTo(baseForm.getStartDate());
        assertThat(toSave.getDueDate()).isEqualTo(baseForm.getDueDate());
        assertThat(toSave.getRequestDate()).isNotNull();
    }

    @Test
    @DisplayName("getRentalsForUser: 소유자·대여자 ID에 매칭되는 대여만 조회된다")
    void getRentalsForUser_filtersByOwnerOrRenter() {
        // given
        Rental r1 = Rental.builder().rentalId(1L).ownerId(10L).renterId(99L).build();
        Rental r2 = Rental.builder().rentalId(2L).ownerId(77L).renterId(10L).build();
        given(rentalRepository.findAllByOwnerIdOrRenterId(10L, 10L))
                .willReturn(Arrays.asList(r1, r2));
        doReturn("http://dummy.url/image.jpg")
                .when(fileStorageService).generatePresignedUrl(any());

        MemberDto user = mock(MemberDto.class);
        given(user.getId()).willReturn(10L);

        // when
        List<RentalDto> list = rentalService.getRentalsForUser(user);

        // then
        assertThat(list)
                .extracting(RentalDto::getRentalId)
                .containsExactlyInAnyOrder(1L, 2L);

        verify(rentalRepository).findAllByOwnerIdOrRenterId(10L, 10L);
        verify(fileStorageService, times(2)).generatePresignedUrl(null);
    }

    @Test
    @DisplayName("getRental: 소유자·대여자가 아니면 SecurityException")
    void getRental_throwsIfNotOwnerOrRenter() {
        // given
        Rental stored = Rental.builder()
                .rentalId(5L).ownerId(10L).renterId(20L).build();
        given(rentalRepository.findById(5L))
                .willReturn(Optional.of(stored));

        MemberDto stranger = mock(MemberDto.class);
        given(stranger.getId()).willReturn(999L);

        // when & then
        assertThatThrownBy(() -> rentalService.getRental(5L, stranger))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("조회 권한이 없습니다.");
    }

    @Test
    @DisplayName("getRental: 소유자와 대여자는 정상 조회 가능")
    void getRental_succeedsForOwnerAndRenter() {
        // given
        Rental stored = Rental.builder()
                .rentalId(5L).ownerId(10L).renterId(20L).build();
        given(rentalRepository.findById(5L))
                .willReturn(Optional.of(stored));
        doReturn("http://dummy")
                .when(fileStorageService).generatePresignedUrl(any());

        MemberDto owner = mock(MemberDto.class);
        given(owner.getId()).willReturn(10L);
        MemberDto renter = mock(MemberDto.class);
        given(renter.getId()).willReturn(20L);

        // when
        RentalDto dto1 = rentalService.getRental(5L, owner);
        RentalDto dto2 = rentalService.getRental(5L, renter);

        // then
        assertThat(dto1.getRentalId()).isEqualTo(5L);
        assertThat(dto2.getRentalId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("cancel: 대여자만 취소할 수 있으며, 취소 후 상태 CANCELLED")
    void cancel_allowsOnlyRenter() {
        // given
        Rental stored = Rental.builder()
                .rentalId(7L).renterId(20L).build();
        given(rentalRepository.findById(7L))
                .willReturn(Optional.of(stored));

        // when
        rentalService.cancel(7L, 20L);
        // then
        assertThat(stored.getStatus()).isEqualTo(RentalStatusEnum.CANCELLED);

        // 그리고 권한 없는 사용자
        assertThatThrownBy(() -> rentalService.cancel(7L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("취소 권한이 없습니다.");
    }

    @Test
    @DisplayName("returnToLocker: 대여자가 lockerId 재지정 후 RETURNED_TO_LOCKER, returnedAt, 이미지 URL 설정 및 예외")
    void returnToLocker_assignsLockerAndSetsStatus() {
        // given
        Rental stored = Rental.builder()
                .rentalId(13L).renterId(20L).build();
        given(rentalRepository.findById(13L))
                .willReturn(Optional.of(stored));
        MockMultipartFile file = new MockMultipartFile(
                "returnImage", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "data".getBytes());
        given(fileStorageService.store(file))
                .willReturn("http://cdn.returned/photo.jpg");

        // when
        rentalService.returnToLocker(13L, 20L, 444L, file);

        // then
        assertThat(stored.getStatus()).isEqualTo(RentalStatusEnum.RETURNED_TO_LOCKER);
        assertThat(stored.getLockerId()).isEqualTo(444L);
        assertThat(stored.getReturnedAt()).isNotNull();
        assertThat(stored.getReturnImageUrl()).isEqualTo("http://cdn.returned/photo.jpg");

        // renter 권한 없으면
        assertThatThrownBy(() ->
                rentalService.returnToLocker(13L, 999L, 444L, file)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("권한이 없습니다.");

        // 이미지 없으면
        assertThatThrownBy(() ->
                rentalService.returnToLocker(13L, 20L, 444L, null)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("반납 사진이 없습니다.");
    }

    // 나머지 메서드들도 동일한 패턴으로 Mockito 만 사용해 테스트하시면 됩니다.
}
