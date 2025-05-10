package com.capstone.rentit.inquiry.repository;

import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.inquiry.dto.InquirySearchForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomInquiryRepository {
    Page<Inquiry> search(InquirySearchForm form, Pageable pageable);
}
