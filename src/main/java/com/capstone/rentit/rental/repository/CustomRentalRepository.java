package com.capstone.rentit.rental.repository;

import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CustomRentalRepository {
    Page<Rental> findAllByUserIdAndStatuses(Long userId, List<RentalStatusEnum> statuses, Pageable pageable);
    List<Rental> findEligibleRentals(Long memberId, RentalLockerAction action);
    Page<Rental> findAllByStatuses(List<RentalStatusEnum> statuses, Pageable pageable);
    Optional<Rental> findByIdWithItem(Long rentalId);
}
