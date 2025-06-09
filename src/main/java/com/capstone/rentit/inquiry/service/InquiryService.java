package com.capstone.rentit.inquiry.service;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.inquiry.dto.*;
import com.capstone.rentit.inquiry.exception.InquiryNotFoundException;
import com.capstone.rentit.inquiry.repository.InquiryRepository;
import com.capstone.rentit.inquiry.type.InquiryType;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.notification.exception.NotificationAccessDenied;
import com.capstone.rentit.notification.service.NotificationService;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.dto.RentalDto;
import com.capstone.rentit.rental.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final RentalService rentalService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public Long createInquiry(Long memberId, InquiryCreateForm form) {
        Inquiry entity = Inquiry.create(memberId, form);
        return inquiryRepository.save(entity).getInquiryId();
    }

    public Long createDamageReport(Long memberId, DamageReportCreateForm form) {

        RentalDto rental = rentalService.getRental(form.rentalId(), memberId);
        Inquiry inquiry = Inquiry.create(memberId, rental.getRenterId(), form);

        Inquiry saved = inquiryRepository.save(inquiry);

        notificationService.notifyItemDamagedRequest(rental.getRentalId());

        return saved.getInquiryId();
    }

    @Transactional(readOnly = true)
    public InquiryResponse getInquiry(Long id) {
        Inquiry inquiry = findInquiry(id);

        List<String> urls = null;
        List<String> keys = inquiry.getDamageImageKeys();
        if (keys != null) {
            urls = keys.stream()
                    .map(fileStorageService::generatePresignedUrl)
                    .toList();
        }

        return InquiryResponse.fromEntity(inquiry, urls);
    }

    @Transactional(readOnly = true)
    public Page<InquiryResponse> search(InquirySearchForm form, MemberRoleEnum role, Long memberId, Pageable pageable) {

        Page<Inquiry> page = inquiryRepository.search(form, role, memberId, pageable);

        return page.map(inquiry -> {
            List<String> urls = null;
            List<String> keys = inquiry.getDamageImageKeys();

            if (keys != null) {
                urls = keys.stream()
                        .map(fileStorageService::generatePresignedUrl)
                        .toList();
            }

            return InquiryResponse.fromEntity(inquiry, urls);
        });
    }

    public void deleteInquiry(Long inquiryId) {
        inquiryRepository.deleteById(inquiryId);
    }

    public void answerDamageReport(Long inquiryId, Long responderId, InquiryAnswerForm form) {
        Inquiry inquiry = findInquiry(inquiryId);
        assertTargetId(inquiry.getTargetMemberId(), responderId);

        inquiry.answerInquiry(form);
        notificationService.notifyItemDamagedResponse(inquiry);
    }

    public void answerInquiry(Long inquiryId, InquiryAnswerForm form){
        Inquiry inquiry = findInquiry(inquiryId);
        inquiry.answerInquiry(form);

        notificationService.notifyInquiryResponse(inquiry);
    }

    public void markProcessed(Long inquiryId) {
        Inquiry inquiry = findInquiry(inquiryId);
        inquiry.markProcessed();
    }

    private Inquiry findInquiry(Long id){
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new InquiryNotFoundException("존재하지 않는 문의 ID"));
    }

    private void assertTargetId(Long targetMemberId, Long responderId){
        if (!targetMemberId.equals(responderId)) {
            throw new NotificationAccessDenied("해당 신고에 대한 권한이 없습니다.");
        }
    }
}
