package com.capstone.rentit.payment.repository;

import com.capstone.rentit.payment.domain.Payment;
import com.capstone.rentit.payment.dto.PaymentSearchForm;

import java.util.List;

public interface CustomPaymentRepository {
    List<Payment> findByCond(PaymentSearchForm form);
}
