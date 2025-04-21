package com.capstone.rentit.rental.service;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.dto.RentalDto;
import com.capstone.rentit.rental.dto.RentalRequestForm;
import com.capstone.rentit.rental.repository.RentalRepository;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalService {

    private final RentalRepository rentalRepository;
    private final FileStorageService fileStorageService;

    /** 대여 요청 생성 */
    public Long requestRental(RentalRequestForm form) {
        Rental rental = Rental.builder()
                .itemId(form.getItemId())
                .ownerId(form.getOwnerId())
                .renterId(form.getRenterId())
                .requestDate(LocalDateTime.now())
                .startDate(form.getStartDate())
                .status(RentalStatusEnum.REQUESTED)
                .dueDate(form.getDueDate())
                .build();
        return rentalRepository.save(rental).getRentalId();
    }

    /** 현재 사용자(소유자/대여자)의 대여 목록 조회 */
    @Transactional(readOnly = true)
    public List<RentalDto> getRentalsForUser(MemberDto loginMember) {
        Long userId = loginMember.getId();
        List<Rental> list = rentalRepository.findAllByOwnerIdOrRenterId(userId, userId);
        return list.stream().map(r -> RentalDto.fromEntity(
                r, fileStorageService.generatePresignedUrl(r.getReturnImageUrl()))
                )
                .collect(Collectors.toList());
    }

    /** 단일 대여 조회 */
    @Transactional(readOnly = true)
    public RentalDto getRental(Long rentalId, MemberDto loginMember) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대여 정보"));
        // 권한 체크: 소유자 또는 대여자만 조회 가능
        Long userId = loginMember.getId();
        if (!rental.getOwnerId().equals(userId) && !rental.getRenterId().equals(userId)) {
            throw new SecurityException("조회 권한이 없습니다.");
        }
        return RentalDto.fromEntity(rental, fileStorageService.generatePresignedUrl(rental.getReturnImageUrl()));
    }

    /** 4) 대여 승인 (소유자/관리자) */
    public void approve(Long rentalId) {
        Rental r = findOrThrow(rentalId);
        r.approve(LocalDateTime.now());
    }

    /** 5) 대여 거절 (소유자/관리자) */
    public void reject(Long rentalId) {
        Rental r = findOrThrow(rentalId);
        r.reject(LocalDateTime.now());
    }

    /** 6) 대여 취소 (반드시 대여자만) */
    public void cancel(Long rentalId, Long requesterId) {
        Rental r = findOrThrow(rentalId);
        if (!r.getRenterId().equals(requesterId)) {
            throw new IllegalArgumentException("취소 권한이 없습니다.");
        }
        r.cancel();
    }

    /** 관리자: 특정 사용자 대여 목록 조회 */
    @Transactional(readOnly = true)
    public List<RentalDto> getRentalsByUser(Long userId) {
        List<Rental> list = rentalRepository.findAllByOwnerIdOrRenterId(userId, userId);
        return list.stream().map(r -> RentalDto.fromEntity(
                        r, fileStorageService.generatePresignedUrl(r.getReturnImageUrl()))
                )
                .collect(Collectors.toList());
    }


    /** 7) 소유자가 사물함에 물건을 맡길 때 */
    public void dropOffToLocker(Long rentalId, Long ownerId, Long lockerId) {
        Rental r = findOrThrow(rentalId);
        if (!r.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        r.assignLocker(lockerId);
        r.dropOffByOwner(LocalDateTime.now());
    }

    /** 8) 대여자가 사물함에서 픽업할 때 */
    public void pickUpByRenter(Long rentalId, Long renterId) {
        Rental r = findOrThrow(rentalId);
        if (!r.getRenterId().equals(renterId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        r.clearLocker();
        r.pickUpByRenter(LocalDateTime.now());
    }

    /** 9) 대여자가 사물함에 물건을 반환할 때 */
    public void returnToLocker(Long rentalId, Long renterId, Long lockerId, MultipartFile returnImage) {
        Rental r = findOrThrow(rentalId);
        if (!r.getRenterId().equals(renterId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        r.assignLocker(lockerId);
        r.returnToLocker(LocalDateTime.now());

        if(returnImage == null) {
            throw new IllegalArgumentException("반납 사진이 없습니다.");
        }
        String objectKey = fileStorageService.store(returnImage);
        r.uploadReturnImageUrl(objectKey);
    }

    /** 10) 소유자가 사물함에서 물건을 회수할 때 (대여 완료) */
    public void retrieveByOwner(Long rentalId, Long ownerId) {
        Rental r = findOrThrow(rentalId);
        if (!r.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        r.clearLocker();
        r.retrieveByOwner(LocalDateTime.now());
    }

    private Rental findOrThrow(Long id) {
        return rentalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 대여 정보"));
    }
}
