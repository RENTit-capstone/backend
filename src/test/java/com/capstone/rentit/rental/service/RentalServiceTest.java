package com.capstone.rentit.rental.service;

import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.dto.RentalDto;
import com.capstone.rentit.rental.dto.RentalRequestForm;
import com.capstone.rentit.rental.repository.RentalRepository;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class RentalServiceTest {

    @Autowired
    private RentalService rentalService;

    @Autowired
    private RentalRepository rentalRepository;

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
    @DisplayName("requestRental: 요청한 필드대로 Rental 엔티티가 저장된다")
    void requestRental_savesCorrectEntity() {
        Long id = rentalService.requestRental(baseForm);

        Rental r = rentalRepository.findById(id)
                .orElseThrow(() -> new AssertionError("Rental not found"));

        assertThat(r.getItemId()).isEqualTo(100L);
        assertThat(r.getOwnerId()).isEqualTo(10L);
        assertThat(r.getRenterId()).isEqualTo(20L);
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.REQUESTED);
        assertThat(r.getRequestDate()).isNotNull();
        assertThat(r.getStartDate()).isEqualTo(baseForm.getStartDate());
        assertThat(r.getDueDate()).isEqualTo(baseForm.getDueDate());
        assertThat(r.getLockerId()).isNull();
    }

    @Test
    @DisplayName("getRentalsForUser: 소유자·대여자 ID에 매칭되는 대여만 조회된다")
    void getRentalsForUser_filtersByOwnerOrRenter() {
        // owner=10,renter=99
        RentalRequestForm f1 = new RentalRequestForm();
        f1.setItemId(101L);
        f1.setOwnerId(10L);
        f1.setRenterId(99L);
        f1.setStartDate(LocalDateTime.now().plusDays(2));
        f1.setDueDate(LocalDateTime.now().plusDays(8));
        Long id1 = rentalService.requestRental(f1);

        // owner=77,renter=10
        RentalRequestForm f2 = new RentalRequestForm();
        f2.setItemId(102L);
        f2.setOwnerId(77L);
        f2.setRenterId(10L);
        f2.setStartDate(LocalDateTime.now().plusDays(3));
        f2.setDueDate(LocalDateTime.now().plusDays(9));
        Long id2 = rentalService.requestRental(f2);

        MemberDto user10 = Mockito.mock(MemberDto.class);
        when(user10.getId()).thenReturn(10L);

        List<RentalDto> list = rentalService.getRentalsForUser(user10);
        assertThat(list)
                .extracting(RentalDto::getRentalId)
                .containsExactlyInAnyOrder(id1, id2);
    }

    @Test
    @DisplayName("getRental: 소유자·대여자가 아니면 SecurityException")
    void getRental_throwsIfNotOwnerOrRenter() {
        Long id = rentalService.requestRental(baseForm);

        MemberDto stranger = Mockito.mock(MemberDto.class);
        when(stranger.getId()).thenReturn(999L);

        assertThrows(SecurityException.class,
                () -> rentalService.getRental(id, stranger));
    }

    @Test
    @DisplayName("getRental: 소유자와 대여자는 정상 조회 가능")
    void getRental_succeedsForOwnerAndRenter() {
        Long id = rentalService.requestRental(baseForm);

        MemberDto owner = Mockito.mock(MemberDto.class);
        when(owner.getId()).thenReturn(10L);
        MemberDto renter = Mockito.mock(MemberDto.class);
        when(renter.getId()).thenReturn(20L);

        RentalDto dto1 = rentalService.getRental(id, owner);
        RentalDto dto2 = rentalService.getRental(id, renter);

        assertThat(dto1.getRentalId()).isEqualTo(id);
        assertThat(dto2.getRentalId()).isEqualTo(id);
    }

    @Test
    @DisplayName("approve: 상태가 APPROVED로 변경되고 approvedDate 설정됨")
    void approve_setsApprovedState() {
        Long id = rentalService.requestRental(baseForm);
        rentalService.approve(id);

        Rental r = rentalRepository.findById(id).get();
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.APPROVED);
        assertThat(r.getApprovedDate()).isNotNull();
    }

    @Test
    @DisplayName("reject: 상태가 REJECTED로 변경되고 rejectedDate 설정됨")
    void reject_setsRejectedState() {
        Long id = rentalService.requestRental(baseForm);
        rentalService.reject(id);

        Rental r = rentalRepository.findById(id).get();
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.REJECTED);
        assertThat(r.getRejectedDate()).isNotNull();
    }

    @Test
    @DisplayName("cancel: 대여자만 취소할 수 있으며, 취소 후 상태 CANCELLED")
    void cancel_allowsOnlyRenter() {
        Long id = rentalService.requestRental(baseForm);

        // 정상 취소
        rentalService.cancel(id, 20L);
        assertThat(rentalRepository.findById(id).get().getStatus())
                .isEqualTo(RentalStatusEnum.CANCELLED);

        // 권한 없는 사용자
        assertThrows(IllegalArgumentException.class,
                () -> rentalService.cancel(id, 999L));
    }

    @Test
    @DisplayName("getRentalsByUser: 관리자용 특정 사용자 거래 조회")
    void getRentalsByUser_returnsAllMatches() {
        Long a = rentalService.requestRental(baseForm);

        RentalRequestForm f2 = new RentalRequestForm();
        f2.setItemId(200L);
        f2.setOwnerId(20L);
        f2.setRenterId(30L);
        f2.setStartDate(LocalDateTime.now().plusDays(4));
        f2.setDueDate(LocalDateTime.now().plusDays(10));
        Long b = rentalService.requestRental(f2);

        List<RentalDto> result = rentalService.getRentalsByUser(20L);
        assertThat(result)
                .extracting(RentalDto::getRentalId)
                .containsExactlyInAnyOrder(a, b);
    }

    @Test
    @DisplayName("dropOffToLocker: 소유자가 lockerId 지정 후 LEFT_IN_LOCKER, leftAt 설정")
    void dropOffToLocker_assignsLockerAndSetsStatus() {
        Long id = rentalService.requestRental(baseForm);

        rentalService.dropOffToLocker(id, 10L, 555L);

        Rental r = rentalRepository.findById(id).get();
        assertThat(r.getLockerId()).isEqualTo(555L);
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.LEFT_IN_LOCKER);
        assertThat(r.getLeftAt()).isNotNull();

        assertThrows(IllegalArgumentException.class,
                () -> rentalService.dropOffToLocker(id, 999L, 123L));
    }

    @Test
    @DisplayName("pickUpByRenter: lockerId 클리어 후 PICKED_UP, pickedUpAt 설정")
    void pickUpByRenter_clearsLockerAndSetsStatus() {
        Long id = rentalService.requestRental(baseForm);
        // 먼저 소유자가 맡김
        rentalService.dropOffToLocker(id, 10L, 777L);

        rentalService.pickUpByRenter(id, 20L);

        Rental r = rentalRepository.findById(id).get();
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.PICKED_UP);
        assertThat(r.getPickedUpAt()).isNotNull();
        assertThat(r.getLockerId()).isNull();

        assertThrows(IllegalArgumentException.class,
                () -> rentalService.pickUpByRenter(id, 888L));
    }

    @Test
    @DisplayName("returnToLocker: 대여자가 lockerId 재지정 후 RETURNED_TO_LOCKER, returnedAt 설정")
    void returnToLocker_assignsLockerAndSetsStatus() {
        Long id = rentalService.requestRental(baseForm);

        rentalService.returnToLocker(id, 20L, 444L);

        Rental r = rentalRepository.findById(id).get();
        assertThat(r.getLockerId()).isEqualTo(444L);
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.RETURNED_TO_LOCKER);
        assertThat(r.getReturnedAt()).isNotNull();

        assertThrows(IllegalArgumentException.class,
                () -> rentalService.returnToLocker(id, 999L, 123L));
    }

    @Test
    @DisplayName("retrieveByOwner: lockerId 클리어 후 COMPLETED, retrievedAt 설정")
    void retrieveByOwner_clearsLockerAndCompletes() {
        Long id = rentalService.requestRental(baseForm);
        // 흐름: 맡기 → 반납 → 회수
        rentalService.dropOffToLocker(id, 10L, 333L);
        rentalService.returnToLocker(id, 20L, 333L);

        rentalService.retrieveByOwner(id, 10L);

        Rental r = rentalRepository.findById(id).get();
        assertThat(r.getLockerId()).isNull();
        assertThat(r.getStatus()).isEqualTo(RentalStatusEnum.COMPLETED);
        assertThat(r.getRetrievedAt()).isNotNull();

        assertThrows(IllegalArgumentException.class,
                () -> rentalService.retrieveByOwner(id, 999L));
    }
}