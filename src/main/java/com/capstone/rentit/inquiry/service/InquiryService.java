package com.capstone.rentit.inquiry.service;

import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.inquiry.dto.InquiryAnswerForm;
import com.capstone.rentit.inquiry.dto.InquiryCreateForm;
import com.capstone.rentit.inquiry.dto.InquiryResponse;
import com.capstone.rentit.inquiry.dto.InquirySearchForm;
import com.capstone.rentit.inquiry.exception.InquiryNotFoundException;
import com.capstone.rentit.inquiry.repository.InquiryRepository;
import com.capstone.rentit.inquiry.type.InquiryType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    public Long createInquiry(InquiryCreateForm form) {
        Inquiry entity = Inquiry.builder()
                .memberId(form.memberId())
                .title(form.title())
                .content(form.content())
                .type(form.type())
                .build();                 // processed 는 기본값 false
        return inquiryRepository.save(entity).getInquiryId();
    }

    @Transactional(readOnly = true)
    public InquiryResponse getInquiry(Long id) {
        Inquiry inquiry = findInquiry(id);
        return InquiryResponse.fromEntity(inquiry);
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> getInquiries(Long memberId, InquiryType type) {
        List<Inquiry> list = inquiryRepository.findByMemberId(memberId);
        if(type == null){
            return list.stream().map(InquiryResponse::fromEntity).toList();
        }
        return list.stream().filter(inq -> inq.getType() == type)
                .map(InquiryResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<InquiryResponse> search(InquirySearchForm form, Pageable pageable) {

        Page<Inquiry> page = inquiryRepository.search(form, pageable);

        return page.map(InquiryResponse::fromEntity);
    }

    public void deleteInquiry(Long inquiryId) {
        inquiryRepository.deleteById(inquiryId);
    }

    public void answerInquiry(Long inquiryId, InquiryAnswerForm form){
        Inquiry inquiry = findInquiry(inquiryId);
        inquiry.answerInquiry(form);
    }

    public void markProcessed(Long inquiryId) {
        Inquiry inquiry = findInquiry(inquiryId);
        inquiry.markProcessed();
    }

    private Inquiry findInquiry(Long id){
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new InquiryNotFoundException("존재하지 않는 문의 ID"));
    }
}
