package com.capstone.rentit.rental.repository;

import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long>, CustomRentalRepository {

    List<Rental> findAllByRenterId(Long renterId);

    List<Rental> findAllByOwnerId(Long ownerId);

    List<Rental> findAllByOwnerIdOrRenterId(Long ownerId, Long renterId);

    List<Rental> findByStartDate(LocalDate startDate);
    List<Rental> findByDueDate(LocalDate endDate);

    Optional<Rental> findTopByItemIdAndStatus(Long itemId, RentalStatusEnum status);
}
