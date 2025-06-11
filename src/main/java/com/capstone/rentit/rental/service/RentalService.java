package com.capstone.rentit.rental.service;

import com.capstone.rentit.item.exception.ItemNotFoundException;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.repository.ItemRepository;
import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.notification.service.NotificationService;
import com.capstone.rentit.payment.domain.Wallet;
import com.capstone.rentit.payment.dto.LockerPaymentRequest;
import com.capstone.rentit.payment.dto.RentalPaymentRequest;
import com.capstone.rentit.payment.service.PaymentService;
import com.capstone.rentit.payment.type.PaymentType;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.dto.*;
import com.capstone.rentit.rental.exception.*;
import com.capstone.rentit.rental.repository.RentalRepository;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalService {

    private final RentalRepository rentalRepository;
    private final ItemRepository itemRepository;
    private final FileStorageService fileStorageService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    /** 대여 요청 생성 */
    public Long requestRental(RentalRequestForm form) {
        Item item = findItem(form.getItemId());
        assertNotOwner(item, form.getRenterId());
        assertItemAvailable(item);
        paymentService.assertCheckBalance(form.getRenterId(), item.getPrice());

        Rental rental = Rental.create(form);
        rental.setItem(item);

        Long rentalId = rentalRepository.save(rental).getRentalId();
        paymentService.requestRentalFee(new RentalPaymentRequest(rental.getRenterId(), rental.getOwnerId(), item.getPrice()), rentalId);

        item.updateRequested();
        notificationService.notifyRentRequest(rentalId);
        return rentalId;
    }

    /** 현재 사용자(소유자/대여자)의 대여 목록 조회 */
    @Transactional(readOnly = true)
    public Page<RentalDto> getRentalsForUser(MemberDto loginMember, RentalSearchForm searchForm, Pageable pageable) {
        Long userId = loginMember.getMemberId();
        Page<Rental> rentals = rentalRepository.findAllByUserIdAndStatuses(userId, searchForm.getStatuses(), pageable);
        return rentals.map(r ->
                RentalDto.fromEntity(r, fileStorageService.generatePresignedUrl(r.getReturnImageUrl()))
        );
    }

    /** 단일 대여 조회 */
    @Transactional(readOnly = true)
    public RentalDto getRental(Long rentalId, Long memberId) {
        Rental rental = findRental(rentalId);
        assertOwnerOrRenter(rental, memberId);

        return RentalDto.fromEntity(rental, fileStorageService.generatePresignedUrl(rental.getReturnImageUrl()));
    }

    /** 4) 대여 승인 (소유자/관리자) */
    public void approve(Long rentalId) {
        Rental r = findRental(rentalId);
        r.approve(LocalDateTime.now());

        Item item = findItem(r.getItemId());
        item.updateOut();

        paymentService.payRentalFee(rentalId);
        notificationService.notifyRequestAccepted(rentalId);
    }

    /** 5) 대여 거절 (소유자/관리자) */
    public void reject(Long rentalId) {
        Rental r = findRental(rentalId);
        assertBeforeApproved(r);
        r.reject(LocalDateTime.now());

        paymentService.cancelPayment(rentalId);
        notificationService.notifyRentRejected(rentalId);
    }

    /** 6) 대여 취소 (반드시 대여자만) */
    public void cancel(Long rentalId, Long renterId) {
        Rental r = findRental(rentalId);
        assertRenter(r, renterId);
        assertBeforeApproved(r);

        r.cancel();
        Item item = findItem(r.getItemId());
        item.updateAvailable();

        paymentService.cancelPayment(rentalId);
        notificationService.notifyRequestCancel(rentalId);
    }

    private void assertBeforeApproved(Rental r) {
        if(r.getStatus() != RentalStatusEnum.REQUESTED){
            throw new RentalCantCanceledException("승인된 대여는 취소할 수 없습니다.");
        }
    }

    /** 관리자: 특정 사용자 대여 목록 조회 */
    @Transactional(readOnly = true)
    public List<RentalBriefResponse> getRentalsByUser(Long userId) {
        List<Rental> list = rentalRepository.findAllByOwnerIdOrRenterId(userId, userId);
        return list.stream().map(r ->
                        RentalBriefResponse.fromEntity(r, "", false, fileStorageService.generatePresignedUrl(r.getReturnImageUrl()))
                )
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<RentalBriefResponse> getAllRentals(RentalSearchForm searchForm, Pageable pageable) {
        List<RentalStatusEnum> statuses = searchForm.getStatuses();
        Page<Rental> rentalsPage;

        if (statuses == null || statuses.isEmpty()) {
            rentalsPage = rentalRepository.findAll(pageable);
        } else {
            rentalsPage = rentalRepository.findAllByStatuses(statuses, pageable);
        }

        return rentalsPage.map(r ->
            RentalBriefResponse.fromEntity(r, "", false, fileStorageService.generatePresignedUrl(r.getReturnImageUrl())));
    }

    public List<RentalBriefResponseForLocker> findEligibleRentals(Long memberId, RentalLockerAction action) {
        Wallet wallet = paymentService.findWallet(memberId);
        List<Rental> list = rentalRepository.findEligibleRentals(memberId, action);
        return list.stream().map(r ->
                RentalBriefResponseForLocker.fromEntity(r,
                        paymentService.getLockerFeeByAction(action, r, LocalDateTime.now()),
                        wallet.getBalance())).toList();
    }

    /** 7) 소유자가 사물함에 물건을 맡길 때 */
    public void dropOffToLocker(Long rentalId, Long ownerId, Long deviceId, Long lockerId) {
        Rental r = findRental(rentalId);
        assertOwner(r, ownerId);

        r.assignLocker(deviceId, lockerId);
        r.dropOffByOwner(LocalDateTime.now());

        notificationService.notifyItemPlaced(rentalId, deviceId, lockerId);
    }

    /** 8) 대여자가 사물함에서 픽업할 때 */
    public void pickUpByRenter(Long rentalId, Long renterId, long fee) {
        Rental r = findRental(rentalId);
        assertRenter(r, renterId);

        r.clearLocker();
        r.pickUpByRenter(LocalDateTime.now());

        paymentService.payLockerFee(
                new LockerPaymentRequest(renterId, PaymentType.LOCKER_FEE_RENTER, fee));
    }

    /** 9) 대여자가 사물함에 물건을 반환할 때 */
    public void returnToLocker(Long rentalId, Long renterId, Long deviceId, Long lockerId) {
        Rental r = findRental(rentalId);
        assertRenter(r, renterId);

        r.assignLocker(deviceId, lockerId);
        r.returnToLocker(LocalDateTime.now());
        notificationService.notifyItemReturned(rentalId, deviceId, lockerId);
    }

    public void uploadReturnImage(Long rentalId, Long renterId, String returnImageKey) {
        Rental r = findRental(rentalId);
        assertRenter(r, renterId);
        assertReturnState(r);

        assertReturnImage(returnImageKey);
        r.uploadReturnImageUrl(returnImageKey);
    }

    /** 10) 소유자가 사물함에서 물건을 회수할 때 (대여 완료) */
    public void retrieveByOwner(Long rentalId, Long ownerId, long fee) {
        Rental r = findRental(rentalId);
        assertOwner(r, ownerId);

        r.clearLocker();
        r.retrieveByOwner(LocalDateTime.now());

        Item item = findItem(r.getItemId());
        item.updateAvailable();

        paymentService.payLockerFee(
                new LockerPaymentRequest(ownerId, PaymentType.LOCKER_FEE_OWNER, fee));
    }

    private Rental findRental(Long id) {
        return rentalRepository.findById(id)
                .orElseThrow(() -> new RentalNotFoundException("존재하지 않는 대여 정보입니다."));
    }

    private Item findItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("존재하지 않는 물품입니다."));
    }

    private void assertItemAvailable(Item item) {
        if (item.getStatus() == ItemStatusEnum.OUT || item.getStatus() == ItemStatusEnum.REQUESTED) {
            throw new ItemAlreadyRentedException("다른 사람이 대여 중이거나 승인 대기 중인 물품입니다.");
        }
    }

    private void assertOwnerOrRenter(Rental r, Long userId) {
        if (!r.getOwnerId().equals(userId) && !r.getRenterId().equals(userId)) {
            throw new RentalUnauthorizedException("물품 소유자 또는 대여자가 아닙니다.");
        }
    }

    private void assertOwner(Rental r, Long ownerId) {
        if (!r.getOwnerId().equals(ownerId)) {
            throw new RentalUnauthorizedException("물품 소유자가 아닙니다.");
        }
    }

    private void assertRenter(Rental r, Long renterId) {
        if (!r.getRenterId().equals(renterId)) {
            throw new RentalUnauthorizedException("물품 대여자가 아닙니다.");
        }
    }

    private void assertNotOwner(Item item, Long renterId) {
        if (item.getOwnerId().equals(renterId)) {
            throw new RentalUnauthorizedException("자신의 물품에 대여 신청을 할 수 없습니다.");
        }
    }

    private void assertReturnImage(String returnImageKey) {
        if (returnImageKey == null || returnImageKey.isEmpty()) {
            throw new ReturnImageMissingException("물품 반납 사진이 없습니다.");
        }
    }

    private void assertReturnState(Rental r) {
        if(r.getStatus() != RentalStatusEnum.RETURNED_TO_LOCKER && r.getStatus() != RentalStatusEnum.COMPLETED){
            throw new ItemNotReturnedException("반납된 물품이 아닙니다.");
        }
    }
}
