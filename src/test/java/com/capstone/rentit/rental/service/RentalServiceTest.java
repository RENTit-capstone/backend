package com.capstone.rentit.rental.service;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.item.exception.ItemNotFoundException;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.repository.ItemRepository;
import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.notification.service.NotificationService;
import com.capstone.rentit.payment.domain.Wallet;
import com.capstone.rentit.payment.service.PaymentService;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.dto.*;
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
    @Mock PaymentService       paymentService;
    @Mock NotificationService  notificationService;

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

    // ───────────────────────────────────────────────────────────────────────
    // Helpers

    private void stubEmptyRental(Long rentalId) {
        given(rentalRepository.findById(rentalId)).willReturn(Optional.empty());
    }

    private Rental buildBasicRental(Long id, Long ownerId, Long renterId, RentalStatusEnum status) {
        return Rental.builder()
                .rentalId(id)
                .ownerId(ownerId)
                .renterId(renterId)
                .status(status)
                .build();
    }

    private void assertRentalNotFoundOn(Runnable action, Long rentalId) {
        stubEmptyRental(rentalId);
        assertThatThrownBy(action::run)
                .isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("존재하지 않는 대여 정보입니다.");
    }

    // ───────────────────────────────────────────────────────────────────────
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
        Item avail = Item.builder().itemId(100L).ownerId(999L).status(ItemStatusEnum.AVAILABLE).build();
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

    // ───────────────────────────────────────────────────────────────────────
    // ---- getRentalsForUser ----

    @Test
    @DisplayName("getRentalsForUser: Login Member 기준 필터 및 URL 생성")
    void getRentalsForUser_success() {
        Rental r1 = buildBasicRental(1L, 10L, 99L, RentalStatusEnum.APPROVED);
        Rental r2 = buildBasicRental(2L, 77L, 10L, RentalStatusEnum.PICKED_UP);

        RentalSearchForm form = new RentalSearchForm();
        form.setStatuses(List.of(RentalStatusEnum.APPROVED));

        MemberDto user = mock(MemberDto.class);
        given(user.getMemberId()).willReturn(10L);

        Page<Rental> rentalPage = new PageImpl<>(List.of(r1));
        given(rentalRepository.findAllByUserIdAndStatuses(
                10L, form.getStatuses(), unpaged()))
                .willReturn(rentalPage);
        doReturn("signed-url").when(fileStorageService).generatePresignedUrl(null);

        Page<RentalDto> dtoPage = rentalService.getRentalsForUser(user, form, unpaged());
        assertThat(dtoPage).extracting(RentalDto::getRentalId)
                .containsExactlyInAnyOrder(1L);
    }

    // ───────────────────────────────────────────────────────────────────────
    // ---- getRental ----

    @Test
    @DisplayName("getRental: 데이터 없으면 RentalNotFoundException")
    void getRental_notFound() {
        // 'getRental' 메서드를 호출하도록 수정 (기존에 잘못 getRentalsByUser 호출함)
        MemberDto anyUser = mock(MemberDto.class);
        given(anyUser.getMemberId()).willReturn(999L);

        stubEmptyRental(5L);
        assertThatThrownBy(() -> rentalService.getRental(5L, anyUser.getMemberId()))
                .isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("존재하지 않는 대여 정보입니다.");
    }

    @Test
    @DisplayName("getRental: 권한 없으면 RentalUnauthorizedException")
    void getRental_noPermission() {
        Rental r = buildBasicRental(5L, 10L, 20L, RentalStatusEnum.REQUESTED);
        given(rentalRepository.findById(5L)).willReturn(Optional.of(r));

        MemberDto stranger = mock(MemberDto.class);
        given(stranger.getMemberId()).willReturn(999L);

        assertThatThrownBy(() -> rentalService.getRental(5L, stranger.getMemberId()))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("물품 소유자 또는 대여자가 아닙니다.");
    }

    @Test
    @DisplayName("getRental: 소유자·대여자는 정상 조회")
    void getRental_success() {
        Rental r = buildBasicRental(5L, 10L, 20L, RentalStatusEnum.REQUESTED);
        given(rentalRepository.findById(5L)).willReturn(Optional.of(r));
        doReturn("some-url").when(fileStorageService).generatePresignedUrl(null);

        MemberDto owner = mock(MemberDto.class);
        given(owner.getMemberId()).willReturn(10L);
        RentalDto dto1 = rentalService.getRental(5L, owner.getMemberId());
        assertThat(dto1.getRentalId()).isEqualTo(5L);

        MemberDto renter = mock(MemberDto.class);
        given(renter.getMemberId()).willReturn(20L);
        RentalDto dto2 = rentalService.getRental(5L, renter.getMemberId());
        assertThat(dto2.getRentalId()).isEqualTo(5L);
    }

    // ───────────────────────────────────────────────────────────────────────
    // ---- findEligibleRentals ----

    @Test
    @DisplayName("리포지토리에서 조회된 렌탈을 DTO 로 매핑하여 반환한다")
    void findEligibleRentals_mapsEntityToDto() {
        Long memberId = 42L;
        RentalLockerAction action = RentalLockerAction.PICK_UP_BY_RENTER;
        long walletBalance = 20000L;
        long lockerFee     = 1000L;

        Item item = Item.builder()
                .itemId(555L)
                .ownerId(99L)
                .name("test item")
                .build();

        Rental rental = Rental.builder()
                .rentalId(123L)
                .ownerId(99L)
                .renterId(memberId)
                .itemId(item.getItemId())
                .item(item)
                .status(RentalStatusEnum.LEFT_IN_LOCKER)
                .requestDate(LocalDateTime.of(2025,5,1,10,30))
                .dueDate(LocalDateTime.of(2025,5,8,10,30))
                .startDate(LocalDateTime.of(2025,5,2,10,30))
                .build();

        Wallet wallet = Wallet.builder()
                .memberId(memberId)
                .balance(walletBalance)
                .build();

        given(paymentService.findWallet(memberId)).willReturn(wallet);
        given(rentalRepository.findEligibleRentals(memberId, action))
                .willReturn(List.of(rental));
        given(paymentService.getLockerFeeByAction(
                eq(action), eq(rental), any(LocalDateTime.class)))
                .willReturn(lockerFee);

        List<RentalBriefResponseForLocker> dtos =
                rentalService.findEligibleRentals(memberId, action);

        then(paymentService).should().findWallet(memberId);
        then(rentalRepository).should().findEligibleRentals(memberId, action);
        then(paymentService).should()
                .getLockerFeeByAction(eq(action), eq(rental), any(LocalDateTime.class));

        assertThat(dtos)
                .hasSize(1)
                .first()
                .satisfies(dto -> {
                    assertThat(dto.getRentalId()).isEqualTo(123L);
                    assertThat(dto.getItemId()).isEqualTo(555L);
                    assertThat(dto.getBalance()).isEqualTo(walletBalance);
                    assertThat(dto.getFee()).isEqualTo(lockerFee);
                });
    }

    @Test
    @DisplayName("리포지토리 조회 결과가 없으면 빈 리스트를 반환한다")
    void findEligibleRentals_whenNoResults_thenReturnEmpty() {
        Long memberId = 7L;
        RentalLockerAction action = RentalLockerAction.RETURN_BY_RENTER;

        given(rentalRepository.findEligibleRentals(memberId, action))
                .willReturn(Collections.emptyList());

        List<RentalBriefResponseForLocker> dtos =
                rentalService.findEligibleRentals(memberId, action);

        then(rentalRepository).should().findEligibleRentals(memberId, action);
        assertThat(dtos).isEmpty();
    }

    // ───────────────────────────────────────────────────────────────────────
    // ---- approve ----

    @Test
    @DisplayName("approve: 대여 없으면 RentalNotFoundException")
    void approve_notFound() {
        assertRentalNotFoundOn(() -> rentalService.approve(6L), 6L);
    }

    @Test
    @DisplayName("approve: 승인 후 Rental 상태와 Item 상태 변경")
    void approve_success() {
        // Rental 객체에 itemId도 설정하도록 수정
        Rental r = Rental.builder()
                .rentalId(6L)
                .itemId(100L)                     // itemId 추가
                .build();
        given(rentalRepository.findById(6L)).willReturn(Optional.of(r));

        Item i = Item.builder()
                .itemId(100L)
                .status(ItemStatusEnum.AVAILABLE)
                .build();
        given(itemRepository.findById(100L)).willReturn(Optional.of(i));

        rentalService.approve(6L);

        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.APPROVED);
        assertThat(i.getStatus()).isEqualTo(ItemStatusEnum.OUT);
    }

    // ───────────────────────────────────────────────────────────────────────
    // ---- reject ----

    @Test
    @DisplayName("reject: 대여 없으면 RentalNotFoundException")
    void reject_notFound() {
        assertRentalNotFoundOn(() -> rentalService.reject(8L), 8L);
    }

    @Test
    @DisplayName("reject: 거절 후 상태 REJECTED")
    void reject_success() {
        Rental r = buildBasicRental(8L, null, null, RentalStatusEnum.REQUESTED);
        given(rentalRepository.findById(8L)).willReturn(Optional.of(r));

        rentalService.reject(8L);
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.REJECTED);
    }

    @Test
    @DisplayName("reject: 승인된 대여는 RentalCantCanceledException")
    void reject_cantCancel() {
        Rental r = buildBasicRental(8L, null, null, RentalStatusEnum.APPROVED);
        given(rentalRepository.findById(8L)).willReturn(Optional.of(r));

        assertThatThrownBy(() -> rentalService.reject(8L))
                .isInstanceOf(RentalCantCanceledException.class)
                .hasMessageContaining("승인된 대여는 취소할 수 없습니다.");
    }

    // ───────────────────────────────────────────────────────────────────────
    // ---- cancel ----

    @Test
    @DisplayName("cancel: 대여 없으면 RentalNotFoundException")
    void cancel_notFound() {
        assertRentalNotFoundOn(() -> rentalService.cancel(7L, 20L), 7L);
    }

    @Test
    @DisplayName("cancel: 대여자만 취소, 상태 CANCELLED 및 Item AVAILABLE")
    void cancel_success() {
        // Rental 생성 시 itemId를 반드시 설정하도록 수정
        Rental r = Rental.builder()
                .rentalId(7L)
                .itemId(100L)                   // itemId 추가
                .renterId(20L)
                .status(RentalStatusEnum.REQUESTED)
                .build();
        given(rentalRepository.findById(7L)).willReturn(Optional.of(r));

        Item i = Item.builder().itemId(100L).status(ItemStatusEnum.OUT).build();
        given(itemRepository.findById(100L)).willReturn(Optional.of(i));

        rentalService.cancel(7L, 20L);
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.CANCELLED);
        assertThat(i.getStatus()).isEqualTo(ItemStatusEnum.AVAILABLE);

        assertThatThrownBy(() -> rentalService.cancel(7L, 999L))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("물품 대여자가 아닙니다.");
    }

    @Test
    @DisplayName("cancel: 대여 취소 불가능하면 RentalCantCanceledException")
    void cancel_cant() {
        Rental r = buildBasicRental(7L, null, 20L, RentalStatusEnum.APPROVED);
        given(rentalRepository.findById(7L)).willReturn(Optional.of(r));

        assertThatThrownBy(() -> rentalService.cancel(7L, 20L))
                .isInstanceOf(RentalCantCanceledException.class)
                .hasMessageContaining("승인된 대여는 취소할 수 없습니다.");
    }

    // ───────────────────────────────────────────────────────────────────────
    // ---- dropOffToLocker ----

    @Test
    @DisplayName("dropOffToLocker: 대여 없으면 RentalNotFoundException")
    void dropOff_notFound() {
        assertRentalNotFoundOn(() -> rentalService.dropOffToLocker(9L, 10L, 111L, 1L), 9L);
    }

    @Test
    @DisplayName("dropOffToLocker: 소유자만, lockerId 설정 및 상태 LEFT_IN_LOCKER")
    void dropOff_success() {
        Rental r = buildBasicRental(9L, 10L, null, null);
        given(rentalRepository.findById(9L)).willReturn(Optional.of(r));

        rentalService.dropOffToLocker(9L, 10L, 555L, 2L);
        assertThat(r.getDeviceId()).isEqualTo(555L);
        assertThat(r.getLockerId()).isEqualTo(2L);
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.LEFT_IN_LOCKER);

        assertThatThrownBy(() -> rentalService.dropOffToLocker(9L, 999L, 123L, 3L))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("물품 소유자가 아닙니다.");
    }

    // ───────────────────────────────────────────────────────────────────────
    // ---- pickUpByRenter ----

    @Test
    @DisplayName("pickUpByRenter: 대여 없으면 RentalNotFoundException")
    void pickUp_notFound() {
        assertRentalNotFoundOn(() -> rentalService.pickUpByRenter(11L, 20L, 0), 11L);
    }

    @Test
    @DisplayName("pickUpByRenter: 대여자만, lockerId 클리어 및 상태 PICKED_UP")
    void pickUp_success() {
        Rental r = buildBasicRental(11L, null, 20L, null);
        r.assignLocker(777L, 4L);
        given(rentalRepository.findById(11L)).willReturn(Optional.of(r));

        rentalService.pickUpByRenter(11L, 20L, 0);
        assertThat(r.getLockerId()).isNull();
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.PICKED_UP);

        assertThatThrownBy(() -> rentalService.pickUpByRenter(11L, 999L, 0))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("물품 대여자가 아닙니다.");
    }

    // ───────────────────────────────────────────────────────────────────────
    // ---- returnToLocker ----

    @Test
    @DisplayName("returnToLocker: 대여 없으면 RentalNotFoundException")
    void return_notFound() {
        assertRentalNotFoundOn(() -> rentalService.returnToLocker(13L, 20L, 1L, 5L), 13L);
    }

    @Test
    @DisplayName("returnToLocker: 정상 흐름 및 예외 케이스")
    void return_successAndErrors() {
        Rental r = buildBasicRental(13L, null, 20L, null);
        given(rentalRepository.findById(13L)).willReturn(Optional.of(r));

        rentalService.returnToLocker(13L, 20L, 444L, 6L);
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.RETURNED_TO_LOCKER);
        assertThat(r.getDeviceId()).isEqualTo(444L);
        assertThat(r.getLockerId()).isEqualTo(6L);

        assertThatThrownBy(() -> rentalService.returnToLocker(13L, 999L, 444L, 7L))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("물품 대여자가 아닙니다.");
    }

    // ───────────────────────────────────────────────────────────────────────
    // ---- retrieveByOwner ----

    @Test
    @DisplayName("retrieveByOwner: 대여 없으면 RentalNotFoundException")
    void retrieve_notFound() {
        assertRentalNotFoundOn(() -> rentalService.retrieveByOwner(14L, 10L, 0), 14L);
    }

    @Test
    @DisplayName("retrieveByOwner: 정상 회수 후 Item 상태 AVAILABLE 및 예외")
    void retrieve_successAndError() {
        // Rental 객체에 itemId를 반드시 설정하도록 수정
        Rental r = Rental.builder()
                .rentalId(14L)
                .itemId(100L)            // itemId 추가
                .ownerId(10L)
                .build();
        given(rentalRepository.findById(14L)).willReturn(Optional.of(r));
        Item i = Item.builder()
                .itemId(100L)
                .status(ItemStatusEnum.OUT)
                .build();
        given(itemRepository.findById(100L)).willReturn(Optional.of(i));

        // 정상 흐름: ownerId 일치 시
        rentalService.retrieveByOwner(14L, 10L, 0);
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.COMPLETED);
        assertThat(i.getStatus()).isEqualTo(ItemStatusEnum.AVAILABLE);

        // 예외 흐름: ownerId 불일치 시
        assertThatThrownBy(() -> rentalService.retrieveByOwner(14L, 999L, 0))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("물품 소유자가 아닙니다.");
    }

    // ───────────────────────────────────────────────────────────────────────
    // ---- uploadReturnImage ----

    @Test
    @DisplayName("uploadReturnImage: 대여 정보가 없으면 RentalNotFoundException")
    void uploadReturnImage_notFound() {
        stubEmptyRental(1L);

        assertThatThrownBy(() -> rentalService.uploadReturnImage(1L, 10L, "someKey"))
                .isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("존재하지 않는 대여 정보입니다.");
    }

    @Test
    @DisplayName("uploadReturnImage: renterId 가 일치하지 않으면 RentalUnauthorizedException")
    void uploadReturnImage_unauthorized() {
        Rental rental = buildBasicRental(2L, null, 5L, RentalStatusEnum.RETURNED_TO_LOCKER);
        given(rentalRepository.findById(2L)).willReturn(Optional.of(rental));

        assertThatThrownBy(() -> rentalService.uploadReturnImage(2L, 99L, "someKey"))
                .isInstanceOf(RentalUnauthorizedException.class)
                .hasMessageContaining("물품 대여자가 아닙니다.");
    }

    @Test
    @DisplayName("uploadReturnImage: 상태가 RETURNED_TO_LOCKER 가 아니면 ItemNotReturnedException")
    void uploadReturnImage_wrongState() {
        Rental rental = buildBasicRental(3L, null, 7L, RentalStatusEnum.PICKED_UP);
        given(rentalRepository.findById(3L)).willReturn(Optional.of(rental));

        assertThatThrownBy(() -> rentalService.uploadReturnImage(3L, 7L, "someKey"))
                .isInstanceOf(ItemNotReturnedException.class)
                .hasMessageContaining("반납된 물품이 아닙니다.");
    }

    @Test
    @DisplayName("uploadReturnImage: 사진 키가 없으면 ReturnImageMissingException")
    void uploadReturnImage_missingKey() {
        Rental rental = buildBasicRental(5L, null, 10L, RentalStatusEnum.RETURNED_TO_LOCKER);
        given(rentalRepository.findById(5L)).willReturn(Optional.of(rental));

        assertThatThrownBy(() -> rentalService.uploadReturnImage(5L, 10L, ""))
                .isInstanceOf(ReturnImageMissingException.class)
                .hasMessageContaining("물품 반납 사진이 없습니다.");
    }

    @Test
    @DisplayName("uploadReturnImage: 정상 흐름시 Rental.uploadReturnImageUrl 호출")
    void uploadReturnImage_success() {
        Rental rental = Mockito.spy(buildBasicRental(4L, null, 8L, RentalStatusEnum.RETURNED_TO_LOCKER));
        given(rentalRepository.findById(4L)).willReturn(Optional.of(rental));

        String returnKey = "stored/object/key.jpg";
        rentalService.uploadReturnImage(4L, 8L, returnKey);

        then(rental).should().uploadReturnImageUrl(returnKey);
    }

    // ───────────────────────────────────────────────────────────────────────
    // ---- getRentalsByUser (신규 테스트) ----

    @Test
    @DisplayName("getRentalsByUser: 결과 없을 때 빈 리스트 반환")
    void getRentalsByUser_noResults() {
        given(rentalRepository.findAllByOwnerIdOrRenterId(1L, 1L))
                .willReturn(Collections.emptyList());

        List<RentalBriefResponse> result = rentalService.getRentalsByUser(1L);

        then(rentalRepository).should().findAllByOwnerIdOrRenterId(1L, 1L);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getRentalsByUser: 조회된 엔티티를 DTO로 매핑하여 반환")
    void getRentalsByUser_withResults() {
        // --- Rental 엔티티 생성 및 스파이 처리 ---
        Rental r = Rental.builder()
                .rentalId(50L)
                .itemId(200L)
                .ownerId(10L)
                .renterId(20L)
                .status(RentalStatusEnum.APPROVED)
                .requestDate(LocalDateTime.of(2025, 5, 1, 10, 0))
                .startDate(LocalDateTime.of(2025, 5, 2, 10, 0))
                .dueDate(LocalDateTime.of(2025, 5, 9, 10, 0))
                .build();
        Rental rSpy = Mockito.spy(r);

        // --- 연관된 Item, Member 엔티티를 Mockito 스텁으로 제공 ---
        Item item = Item.builder()
                .itemId(200L)
                .name("Sample Item")
                .build();
        Mockito.doReturn(item).when(rSpy).getItem();

        Member ownerMember = Mockito.mock(Member.class);
        given(ownerMember.getNickname()).willReturn("ownerNickname");
        Mockito.doReturn(ownerMember).when(rSpy).getOwnerMember();

        Member renterMember = Mockito.mock(Member.class);
        given(renterMember.getNickname()).willReturn("renterNickname");
        Mockito.doReturn(renterMember).when(rSpy).getRenterMember();

        // --- repository 및 presignedUrl 스텁 설정 ---
        given(rentalRepository.findAllByOwnerIdOrRenterId(10L, 10L))
                .willReturn(List.of(rSpy));
        doReturn("dummy-url").when(fileStorageService).generatePresignedUrl(null);

        // --- 서비스 호출 ---
        List<RentalBriefResponse> result = rentalService.getRentalsByUser(10L);

        // --- 검증 ---
        then(fileStorageService).should().generatePresignedUrl(null);
        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(dto -> {
                    assertThat(dto.getRentalId()).isEqualTo(50L);
                    assertThat(dto.getItemId()).isEqualTo(200L);
                    // ownerName, renterName이 Mockito에서 설정한 nickname으로 매핑되었는지 확인
                    assertThat(dto.getOwnerName()).isEqualTo("ownerNickname");
                    assertThat(dto.getRenterName()).isEqualTo("renterNickname");
                    // isOwner는 항상 false로 설정됨
                    assertThat(dto.isOwner()).isFalse();
                    // thumbnailUrl은 dummy-url
                    assertThat(dto.getReturnImageUrl()).isEqualTo("dummy-url");
                    // 상태 및 날짜 필드도 올바르게 전달되었는지 확인
                    assertThat(dto.getStatus()).isEqualTo(RentalStatusEnum.APPROVED);
                    assertThat(dto.getRequestDate()).isEqualTo(LocalDateTime.of(2025, 5, 1, 10, 0));
                    assertThat(dto.getStartDate()).isEqualTo(LocalDateTime.of(2025, 5, 2, 10, 0));
                    assertThat(dto.getDueDate()).isEqualTo(LocalDateTime.of(2025, 5, 9, 10, 0));
                });
    }

    // ───────────────────────────────────────────────────────────────────────
    // ---- getAllRentals (신규 테스트) ----

    @Test
    @DisplayName("getAllRentals: 상태가 없으면 findAll이 호출되고 빈 페이지 반환")
    void getAllRentals_noStatuses() {
        RentalSearchForm form = new RentalSearchForm();
        form.setStatuses(Collections.emptyList());
        Page<Rental> emptyPage = new PageImpl<>(Collections.emptyList());

        given(rentalRepository.findAll(unpaged())).willReturn(emptyPage);

        Page<RentalBriefResponse> result = rentalService.getAllRentals(form, unpaged());

        then(rentalRepository).should().findAll(unpaged());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllRentals: 상태가 있으면 findAllByStatuses가 호출되고 빈 페이지 반환")
    void getAllRentals_withStatuses() {
        RentalSearchForm form = new RentalSearchForm();
        form.setStatuses(List.of(RentalStatusEnum.REQUESTED));
        Page<Rental> emptyPage = new PageImpl<>(Collections.emptyList());

        given(rentalRepository.findAllByStatuses(eq(form.getStatuses()), any()))
                .willReturn(emptyPage);

        Page<RentalBriefResponse> result = rentalService.getAllRentals(form, unpaged());

        then(rentalRepository).should().findAllByStatuses(form.getStatuses(), unpaged());
        assertThat(result).isEmpty();
    }
}