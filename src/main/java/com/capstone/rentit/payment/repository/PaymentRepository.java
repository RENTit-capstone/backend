package com.capstone.rentit.payment.repository;

import com.capstone.rentit.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, CustomPaymentRepository { }
