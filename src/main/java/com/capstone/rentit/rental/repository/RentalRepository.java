package com.capstone.rentit.rental.repository;

import com.capstone.rentit.rental.domain.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long>, CustomRentalRepository {

    List<Rental> findAllByRenterId(Long renterId);

    List<Rental> findAllByOwnerId(Long ownerId);

    List<Rental> findAllByOwnerIdOrRenterId(Long ownerId, Long renterId);

    List<Rental> findByStartDate(LocalDate startDate);
    List<Rental> findByEndDate(LocalDate endDate);
}
