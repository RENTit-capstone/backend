package com.capstone.rentit.inquiry.repository;

import com.capstone.rentit.inquiry.domain.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long>, CustomInquiryRepository {
    List<Inquiry> findByMemberId(Long memberId);
}
