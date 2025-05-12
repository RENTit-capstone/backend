package com.capstone.rentit.rental.service;

import com.capstone.rentit.item.exception.ItemNotFoundException;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.repository.ItemRepository;
import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.dto.RentalBriefResponseForLocker;
import com.capstone.rentit.rental.dto.RentalDto;
import com.capstone.rentit.rental.dto.RentalRequestForm;
import com.capstone.rentit.rental.dto.RentalSearchForm;
import com.capstone.rentit.rental.exception.*;
import com.capstone.rentit.rental.repository.RentalRepository;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.data.domain.Pageable.unpaged;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock RentalRepository     rentalRepository;
    @Mock ItemRepository       itemRepository;
    @Mock FileStorageService   fileStorageService;

    @InjectMocks RentalService rentalService;

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

    // ---- requestRental ----

    @Test
    @DisplayName("requestRental: 물품이 없으면 ItemNotFoundException")
    void requestRental_itemNotFound() {
        given(itemRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> rentalService.requestRental(baseForm))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("존재하지 않는 물품입니다.");
    }

    @Test
    @DisplayName("requestRental: 물품이 OUT 상태면 ItemAlreadyRentedException")
    void requestRental_alreadyOut() {
        Item outItem = Item.builder().itemId(100L).status(ItemStatusEnum.OUT).build();
        given(itemRepository.findById(100L)).willReturn(Optional.of(outItem));

        assertThatThrownBy(() -> rentalService.requestRental(baseForm))
                .isInstanceOf(ItemAlreadyRentedException.class)
                .hasMessageContaining("이미 대여된 물품입니다.");
    }

    @Test
    @DisplayName("requestRental: 정상 저장 후 ID 반환")
    void requestRental_success() {
        Item avail = Item.builder().itemId(100L).status(ItemStatusEnum.AVAILABLE).build();
        given(itemRepository.findById(100L)).willReturn(Optional.of(avail));

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

        Long id = rentalService.requestRental(baseForm);
        assertThat(id).isEqualTo(1L);

        ArgumentCaptor<Rental> captor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository).save(captor.capture());
        Rental toSave = captor.getValue();
        assertThat(toSave.getItemId()).isEqualTo(100L);
        assertThat(toSave.getOwnerId()).isEqualTo(10L);
        assertThat(toSave.getRenterId()).isEqualTo(20L);
        assertThat(toSave.getStatus()).isEqualTo(RentalStatusEnum.REQUESTED);
    }

    // ---- getRentalsForUser ----

    @Test
    @DisplayName("getRentalsForUser: Login Member 기준 필터 및 URL생성")
    void getRentalsForUser_success() {
        //given
        Rental r1 = Rental.builder().rentalId(1L).ownerId(10L).renterId(99L).status(RentalStatusEnum.APPROVED).build();
        Rental r2 = Rental.builder().rentalId(2L).ownerId(77L).renterId(10L).status(RentalStatusEnum.PICKED_UP).build();

        RentalSearchForm form = new RentalSearchForm();
        form.setStatuses(List.of(RentalStatusEnum.APPROVED));

        MemberDto user = mock(MemberDto.class);
        given(user.getMemberId()).willReturn(10L);

        Page<Rental> rentalPage = new PageImpl<>(
                List.of(r1),
                unpaged(),
                1
        );

        given(rentalRepository.findAllByUserIdAndStatuses(
                10L, form.getStatuses(), unpaged()))
                .willReturn(rentalPage);
        doReturn("signed-url").when(fileStorageService).generatePresignedUrl(null);

        //when
        Page<RentalDto> dtoPage = rentalService.getRentalsForUser(user, form, unpaged());

        //then
        assertThat(dtoPage).extracting(RentalDto::getRentalId)
                .containsExactlyInAnyOrder(1L);
    }

    // ---- getRental ----

    @Test
    @DisplayName("getRental: 데이터 없으면 RentalNotFoundException")
    void getRental_notFound() {
        given(rentalRepository.findById(5L)).willReturn(Optional.empty());

        MemberDto anyUser = mock(MemberDto.class);
        assertThatThrownBy(() -> rentalService.getRental(5L, anyUser))
                .isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("존재하지 않는 대여 정보입니다.");
    }

    @Test
    @DisplayName("getRental: 권한 없으면 RentalUnauthorizedException")
    void getRental_noPermission() {
        Rental r = Rental.builder().rentalId(5L).ownerId(10L).renterId(20L).build();
        given(rentalRepository.findById(5L)).willReturn(Optional.of(r));

        MemberDto stranger = mock(MemberDto.class);
        given(stranger.getMemberId()).willReturn(999L);

        assertThatThrownBy(() -> rentalService.getRental(5L, stranger))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("물품 소유자 또는 대여자가 아닙니다.");
    }

    @Test
    @DisplayName("getRental: 소유자·대여자는 정상 조회")
    void getRental_success() {
        Rental r = Rental.builder().rentalId(5L).ownerId(10L).renterId(20L).build();
        given(rentalRepository.findById(5L)).willReturn(Optional.of(r));
        doReturn("some-url").when(fileStorageService).generatePresignedUrl(null);

        MemberDto owner = mock(MemberDto.class);
        given(owner.getMemberId()).willReturn(10L);
        RentalDto dto1 = rentalService.getRental(5L, owner);
        assertThat(dto1.getRentalId()).isEqualTo(5L);

        MemberDto renter = mock(MemberDto.class);
        given(renter.getMemberId()).willReturn(20L);
        RentalDto dto2 = rentalService.getRental(5L, renter);
        assertThat(dto2.getRentalId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("리포지토리에서 조회된 렌탈을 DTO 로 매핑하여 반환한다")
    void findEligibleRentals_mapsEntityToDto() {
        // given
        Long memberId = 42L;
        RentalLockerAction action = RentalLockerAction.PICK_UP_BY_RENTER;

        Rental sample = Rental.builder()
                .rentalId(123L)
                .ownerId(99L)
                .renterId(memberId)
                .itemId(555L)
                .item(Item.builder().itemId(555L).name("test item").ownerId(99L).build())
                .status(RentalStatusEnum.LEFT_IN_LOCKER)
                .requestDate(LocalDateTime.of(2025,5,1,10,30))
                .dueDate(LocalDateTime.of(2025,5,8,10,30))
                .startDate(LocalDateTime.of(2025,5,2,10,30))
                .build();

        given(rentalRepository.findEligibleRentals(memberId, action))
                .willReturn(List.of(sample));

        // when
        List<RentalBriefResponseForLocker> dtos =
                rentalService.findEligibleRentals(memberId, action);

        // then
        then(rentalRepository).should().findEligibleRentals(memberId, action);
        assertThat(dtos)
                .hasSize(1)
                .first()
                .satisfies(dto -> {
                    assertThat(dto.getRentalId()).isEqualTo(123L);
                    assertThat(dto.getItemId()).isEqualTo(555L);
                });
    }

    @Test
    @DisplayName("리포지토리 조회 결과가 없으면 빈 리스트를 반환한다")
    void findEligibleRentals_whenNoResults_thenReturnEmpty() {
        // given
        Long memberId = 7L;
        RentalLockerAction action = RentalLockerAction.RETURN_BY_RENTER;

        given(rentalRepository.findEligibleRentals(memberId, action))
                .willReturn(Collections.emptyList());

        // when
        List<RentalBriefResponseForLocker> dtos =
                rentalService.findEligibleRentals(memberId, action);

        // then
        then(rentalRepository).should().findEligibleRentals(memberId, action);
        assertThat(dtos).isEmpty();
    }

    // ---- approve ----

    @Test
    @DisplayName("approve: 대여 없으면 RentalNotFoundException")
    void approve_notFound() {
        given(rentalRepository.findById(6L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> rentalService.approve(6L))
                .isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("존재하지 않는 대여 정보입니다.");
    }

    @Test
    @DisplayName("approve: 승인 후 Rental 상태와 Item 상태 변경")
    void approve_success() {
        Rental r = Rental.builder().rentalId(6L).itemId(100L).build();
        given(rentalRepository.findById(6L)).willReturn(Optional.of(r));
        Item i = Item.builder().itemId(100L).status(ItemStatusEnum.AVAILABLE).build();
        given(itemRepository.findById(100L)).willReturn(Optional.of(i));

        rentalService.approve(6L);

        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.APPROVED);
        assertThat(i.getStatus()).isEqualTo(ItemStatusEnum.OUT);
    }

    // ---- reject ----

    @Test
    @DisplayName("reject: 대여 없으면 RentalNotFoundException")
    void reject_notFound() {
        given(rentalRepository.findById(8L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> rentalService.reject(8L))
                .isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("존재하지 않는 대여 정보입니다.");
    }

    @Test
    @DisplayName("reject: 거절 후 상태 REJECTED")
    void reject_success() {
        Rental r = Rental.builder().rentalId(8L).build();
        given(rentalRepository.findById(8L)).willReturn(Optional.of(r));

        rentalService.reject(8L);

        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.REJECTED);
    }

    // ---- cancel ----

    @Test
    @DisplayName("cancel: 대여 없으면 RentalNotFoundException")
    void cancel_notFound() {
        given(rentalRepository.findById(7L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> rentalService.cancel(7L,20L))
                .isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("존재하지 않는 대여 정보입니다.");
    }

    @Test
    @DisplayName("cancel: 대여자만 취소, 상태 CANCELLED 및 Item AVAILABLE")
    void cancel_success() {
        Rental r = Rental.builder()
                .rentalId(7L)
                .itemId(100L)
                .renterId(20L)
                .status(RentalStatusEnum.APPROVED)
                .build();
        given(rentalRepository.findById(7L)).willReturn(Optional.of(r));
        Item i = Item.builder().itemId(100L).status(ItemStatusEnum.OUT).build();
        given(itemRepository.findById(100L)).willReturn(Optional.of(i));

        rentalService.cancel(7L,20L);
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.CANCELLED);
        assertThat(i.getStatus()).isEqualTo(ItemStatusEnum.AVAILABLE);

        assertThatThrownBy(() -> rentalService.cancel(7L,999L))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("물품 대여자가 아닙니다.");
    }

    // ---- dropOffToLocker ----

    @Test
    @DisplayName("dropOffToLocker: 대여 없으면 RentalNotFoundException")
    void dropOff_notFound() {
        given(rentalRepository.findById(9L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> rentalService.dropOffToLocker(9L,10L,111L, 1L))
                .isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("존재하지 않는 대여 정보입니다.");
    }

    @Test
    @DisplayName("dropOffToLocker: 소유자만, lockerId 설정 및 상태 LEFT_IN_LOCKER")
    void dropOff_success() {
        Rental r = Rental.builder().rentalId(9L).ownerId(10L).build();
        given(rentalRepository.findById(9L)).willReturn(Optional.of(r));

        rentalService.dropOffToLocker(9L,10L,555L, 2L);
        assertThat(r.getDeviceId()).isEqualTo(555L);
        assertThat(r.getLockerId()).isEqualTo(2L);
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.LEFT_IN_LOCKER);

        assertThatThrownBy(() -> rentalService.dropOffToLocker(9L,999L,123L, 3L))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("물품 소유자가 아닙니다.");
    }

    // ---- pickUpByRenter ----

    @Test
    @DisplayName("pickUpByRenter: 대여 없으면 RentalNotFoundException")
    void pickUp_notFound() {
        given(rentalRepository.findById(11L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> rentalService.pickUpByRenter(11L,20L))
                .isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("존재하지 않는 대여 정보입니다.");
    }

    @Test
    @DisplayName("pickUpByRenter: 대여자만, lockerId 클리어 및 상태 PICKED_UP")
    void pickUp_success() {
        Rental r = Rental.builder().rentalId(11L).renterId(20L).build();
        r.assignLocker(777L, 4L);
        given(rentalRepository.findById(11L)).willReturn(Optional.of(r));

        rentalService.pickUpByRenter(11L,20L);
        assertThat(r.getLockerId()).isNull();
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.PICKED_UP);

        assertThatThrownBy(() -> rentalService.pickUpByRenter(11L,999L))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("물품 대여자가 아닙니다.");
    }

    // ---- returnToLocker ----

    @Test
    @DisplayName("returnToLocker: 대여 없으면 RentalNotFoundException")
    void return_notFound() {
        given(rentalRepository.findById(13L)).willReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile(
                "f","f.jpg",MediaType.IMAGE_JPEG_VALUE,"x".getBytes());
        assertThatThrownBy(() -> rentalService.returnToLocker(13L,20L,1L, 5L, file))
                .isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("존재하지 않는 대여 정보입니다.");
    }

    @Test
    @DisplayName("returnToLocker: 정상 흐름 및 예외 케이스")
    void return_successAndErrors() {
        Rental r = Rental.builder().rentalId(13L).renterId(20L).build();
        given(rentalRepository.findById(13L)).willReturn(Optional.of(r));
        MockMultipartFile file = new MockMultipartFile(
                "img","img.jpg",MediaType.IMAGE_JPEG_VALUE,"data".getBytes());
        given(fileStorageService.store(file)).willReturn("stored-key");

        rentalService.returnToLocker(13L,20L,444L, 6L, file);
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.RETURNED_TO_LOCKER);
        assertThat(r.getDeviceId()).isEqualTo(444L);
        assertThat(r.getLockerId()).isEqualTo(6L);
        assertThat(r.getReturnImageUrl()).isEqualTo("stored-key");

        assertThatThrownBy(() -> rentalService.returnToLocker(13L,999L,444L, 7L, file))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("물품 대여자가 아닙니다.");
        assertThatThrownBy(() -> rentalService.returnToLocker(13L,20L,444L, 8L,null))
                .isInstanceOf(ReturnImageMissingException.class)
                .hasMessageContaining("물품 반납 사진이 없습니다.");
    }

    // ---- retrieveByOwner ----

    @Test
    @DisplayName("retrieveByOwner: 대여 없으면 RentalNotFoundException")
    void retrieve_notFound() {
        given(rentalRepository.findById(14L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> rentalService.retrieveByOwner(14L,10L))
                .isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("존재하지 않는 대여 정보입니다.");
    }

    @Test
    @DisplayName("retrieveByOwner: 정상 회수 후 Item 상태 AVAILABLE 및 예외")
    void retrieve_successAndError() {
        Rental r = Rental.builder().rentalId(14L).itemId(100L).ownerId(10L).build();
        given(rentalRepository.findById(14L)).willReturn(Optional.of(r));
        Item i = Item.builder().itemId(100L).status(ItemStatusEnum.OUT).build();
        given(itemRepository.findById(100L)).willReturn(Optional.of(i));

        rentalService.retrieveByOwner(14L,10L);
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.COMPLETED);
        assertThat(i.getStatus()).isEqualTo(ItemStatusEnum.AVAILABLE);

        assertThatThrownBy(() -> rentalService.retrieveByOwner(14L,999L))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("물품 소유자가 아닙니다.");
    }

    @Test
    @DisplayName("대여 정보가 없으면 RentalNotFoundException")
    void uploadReturnImage_notFound() {
        // given
        given(rentalRepository.findById(1L)).willReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("returnImage", new byte[]{1,2,3});

        // when & then
        assertThatThrownBy(() -> rentalService.uploadReturnImage(1L, 10L, file))
                .isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("존재하지 않는 대여 정보입니다.");
    }

    @Test
    @DisplayName("renterId 가 일치하지 않으면 RentalUnauthorizedException")
    void uploadReturnImage_unauthorized() {
        // given
        Rental rental = Rental.builder()
                .rentalId(2L)
                .renterId(5L)              // 실제 대여자 ID = 5
                .status(RentalStatusEnum.RETURNED_TO_LOCKER)
                .build();
        given(rentalRepository.findById(2L)).willReturn(Optional.of(rental));
        MockMultipartFile file = new MockMultipartFile("returnImage", new byte[]{1,2,3});

        // when & then
        assertThatThrownBy(() -> rentalService.uploadReturnImage(2L, 99L, file))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("대여자가 아닙니다.");
    }

    @Test
    @DisplayName("상태가 RETURNED_TO_LOCKER 가 아니면 ItemNotReturnedException")
    void uploadReturnImage_wrongState() {
        // given
        Rental rental = Rental.builder()
                .rentalId(3L)
                .renterId(7L)
                .status(RentalStatusEnum.PICKED_UP)  // 아직 RETURNED_TO_LOCKER 아님
                .build();
        given(rentalRepository.findById(3L)).willReturn(Optional.of(rental));
        MockMultipartFile file = new MockMultipartFile("returnImage", new byte[]{1,2,3});

        // when & then
        assertThatThrownBy(() -> rentalService.uploadReturnImage(3L, 7L, file))
                .isInstanceOf(ItemNotReturnedException.class)
                .hasMessageContaining("반납된 물품이 아닙니다.");
    }

    @Test
    @DisplayName("정상 흐름: 파일 저장 후 Rental.uploadReturnImageUrl 호출")
    void uploadReturnImage_success() {
        // given
        Rental rental = Mockito.spy(Rental.builder()
                .rentalId(4L)
                .renterId(8L)
                .status(RentalStatusEnum.RETURNED_TO_LOCKER)
                .build());
        given(rentalRepository.findById(4L)).willReturn(Optional.of(rental));

        MockMultipartFile file = new MockMultipartFile(
                "returnImage",
                "receipt.jpg",
                "image/jpeg",
                "dummy".getBytes()
        );
        given(fileStorageService.store(file)).willReturn("stored/object/key.jpg");

        // when
        rentalService.uploadReturnImage(4L, 8L, file);

        // then
        then(fileStorageService).should().store(file);
        then(rental).should().uploadReturnImageUrl("stored/object/key.jpg");
    }
}
