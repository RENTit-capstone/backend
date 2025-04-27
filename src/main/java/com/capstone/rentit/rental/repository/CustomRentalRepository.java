package com.capstone.rentit.rental.repository;

import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.status.RentalStatusEnum;

import java.util.List;

public interface CustomRentalRepository {
    List<Rental> findAllByUserIdAndStatuses(Long userId, List<RentalStatusEnum> statuses);
}
