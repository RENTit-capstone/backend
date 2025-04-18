package com.capstone.rentit.rental.repository;

import com.capstone.rentit.rental.domain.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {

    /**
     * 대여자(renter) 기준으로 내역 조회
     */
    List<Rental> findAllByRenterId(Long renterId);

    /**
     * 소유자(owner) 기준으로 내역 조회
     */
    List<Rental> findAllByOwnerId(Long ownerId);

    /**
     * 관리자용: 특정 사용자가 소유자거나 대여자인 모든 거래 내역 조회
     */
    List<Rental> findAllByOwnerIdOrRenterId(Long ownerId, Long renterId);
}
