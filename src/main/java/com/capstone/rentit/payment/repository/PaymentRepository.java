package com.capstone.rentit.payment.repository;

import com.capstone.rentit.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, CustomPaymentRepository {
    Optional<Payment> findByRentalId(Long rentalId);
}
