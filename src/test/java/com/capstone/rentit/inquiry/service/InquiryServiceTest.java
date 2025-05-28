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
import com.capstone.rentit.rental.dto.RentalDto;
import com.capstone.rentit.rental.service.RentalService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    /* ---------------- fixture 상수 ---------------- */
    private static final long MEMBER_ID        = 100L;
    private static final long OTHER_ID         = 200L;
    private static final long INQUIRY_ID       = 42L;
    private static final long DAMAGE_INQUIRYID = 55L;
    private static final long RENTAL_ID        = 77L;

    /* ---------------- Mock & SUT ------------------ */
    @Mock InquiryRepository   inquiryRepository;
    @Mock RentalService       rentalService;
    @Mock FileStorageService  fileStorageService;
    @Mock NotificationService notificationService;

    @InjectMocks
    InquiryService inquiryService;

    /* ---------------- 공용 테스트 데이터 ----------- */
    Inquiry inquiry;
    Inquiry damage;

    @BeforeEach
    void init() {
        inquiry = Inquiry.builder()
                .inquiryId(INQUIRY_ID)
                .memberId(MEMBER_ID)
                .type(InquiryType.SERVICE)
                .title("Help")
                .content("Need assistance")
                .processed(false)
                .createdAt(LocalDateTime.now())
                .build();

        damage = Inquiry.builder()
                .inquiryId(DAMAGE_INQUIRYID)
                .memberId(MEMBER_ID)
                .targetMemberId(OTHER_ID)
                .type(InquiryType.DAMAGE)
                .title("Broken item")
                .content("Item damaged")
                .processed(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /* =====================================================================
       1. 생성(create) 계열
       ===================================================================== */
    @Test
    @DisplayName("createInquiry ─ 저장 후 ID 반환")
    void createInquiry_success() {
        // given
        InquiryCreateForm form =
                new InquiryCreateForm("Help", "Need assistance", InquiryType.SERVICE);

        // 저장될 엔티티에 123L 을 부여해 반환
        given(inquiryRepository.save(any(Inquiry.class)))
                .willAnswer(inv -> {
                    Inquiry src = inv.getArgument(0);
                    return Inquiry.builder()
                            .inquiryId(123L)
                            .memberId(src.getMemberId())
                            .type(src.getType())
                            .title(src.getTitle())
                            .content(src.getContent())
                            .processed(src.isProcessed())
                            .createdAt(src.getCreatedAt())
                            .build();
                });

        // when
        Long id = inquiryService.createInquiry(MEMBER_ID, form);

        // then
        assertThat(id).isEqualTo(123L);
        ArgumentCaptor<Inquiry> cap = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryRepository).save(cap.capture());
        assertThat(cap.getValue().getMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("createDamageReport ─ 대여 정보 확인 후 저장")
    void createDamageReport_success() {
        // given
        DamageReportCreateForm form =
                new DamageReportCreateForm(RENTAL_ID, "파손 신고 제목", "파손 신고 내용", List.of("s3/key1.jpg"));
        given(rentalService.getRental(RENTAL_ID, MEMBER_ID))
                .willReturn(RentalDto.builder()
                        .rentalId(RENTAL_ID)
                        .ownerId(OTHER_ID)
                        .build());
        given(inquiryRepository.save(any(Inquiry.class)))
                .willAnswer(inv -> {
                    Inquiry src = inv.getArgument(0);

                    // ID만 우리가 원하는 값으로 넣고 나머지는 그대로 복사
                    return Inquiry.builder()
                            .inquiryId(DAMAGE_INQUIRYID)    // ★ toBuilder() 대신 여기서 설정
                            .memberId(src.getMemberId())
                            .targetMemberId(src.getTargetMemberId())
                            .type(src.getType())
                            .title(src.getTitle())
                            .content(src.getContent())
                            .processed(src.isProcessed())
                            .damageImageKeys(src.getDamageImageKeys())
                            .createdAt(src.getCreatedAt())
                            .build();
                });

        // when
        Long id = inquiryService.createDamageReport(MEMBER_ID, form);

        // then
        assertThat(id).isEqualTo(DAMAGE_INQUIRYID);
        verify(rentalService).getRental(RENTAL_ID, MEMBER_ID);
    }

    /* =====================================================================
       2. 조회(read) 계열
       ===================================================================== */
    @Test
    void getInquiry_found() {
        given(inquiryRepository.findById(INQUIRY_ID)).willReturn(Optional.of(inquiry));

        InquiryResponse resp = inquiryService.getInquiry(INQUIRY_ID);

        assertThat(resp.inquiryId()).isEqualTo(INQUIRY_ID);
    }

    @Test
    void getInquiry_notFound() {
        given(inquiryRepository.findById(INQUIRY_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.getInquiry(INQUIRY_ID))
                .isInstanceOf(InquiryNotFoundException.class);
    }

    @Test
    void search_delegatesAndMaps() {
        InquirySearchForm form = new InquirySearchForm(null, null, null, null);
        Pageable pageable = PageRequest.of(0, 5);

        given(inquiryRepository.search(form, MemberRoleEnum.STUDENT, MEMBER_ID, pageable))
                .willReturn(new PageImpl<>(List.of(inquiry), pageable, 1));

        Page<InquiryResponse> page =
                inquiryService.search(form, MemberRoleEnum.STUDENT, MEMBER_ID, pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(inquiryRepository).search(form, MemberRoleEnum.STUDENT, MEMBER_ID, pageable);
    }

    /* =====================================================================
       3. 답변(answer) 계열
       ===================================================================== */
    @Test
    void answerInquiry_success() {
        given(inquiryRepository.findById(INQUIRY_ID)).willReturn(Optional.of(inquiry));
        InquiryAnswerForm form = new InquiryAnswerForm("답변");

        inquiryService.answerInquiry(INQUIRY_ID, form);

        assertThat(inquiry.getAnswer()).isEqualTo("답변");
        assertThat(inquiry.isProcessed()).isTrue();
    }

    @Test
    void answerInquiry_notFound() {
        given(inquiryRepository.findById(INQUIRY_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                inquiryService.answerInquiry(INQUIRY_ID,
                        new InquiryAnswerForm("x")))
                .isInstanceOf(InquiryNotFoundException.class);
    }

    @Test
    void answerDamageReport_success() {
        given(inquiryRepository.findById(DAMAGE_INQUIRYID)).willReturn(Optional.of(damage));

        inquiryService.answerDamageReport(DAMAGE_INQUIRYID, OTHER_ID,
                new InquiryAnswerForm("sorry"));

        assertThat(damage.getAnswer()).isEqualTo("sorry");
        assertThat(damage.isProcessed()).isTrue();
    }

    @Test
    void answerDamageReport_accessDenied() {
        given(inquiryRepository.findById(DAMAGE_INQUIRYID)).willReturn(Optional.of(damage));

        assertThatThrownBy(() ->
                inquiryService.answerDamageReport(DAMAGE_INQUIRYID, 999L,
                        new InquiryAnswerForm("x")))
                .isInstanceOf(NotificationAccessDenied.class);
    }

    /* =====================================================================
       4. 상태 변경 & 삭제
       ===================================================================== */
    @Test
    void markProcessed_success() {
        given(inquiryRepository.findById(INQUIRY_ID)).willReturn(Optional.of(inquiry));

        inquiryService.markProcessed(INQUIRY_ID);

        assertThat(inquiry.isProcessed()).isTrue();
    }

    @Test
    void markProcessed_notFound() {
        given(inquiryRepository.findById(INQUIRY_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.markProcessed(INQUIRY_ID))
                .isInstanceOf(InquiryNotFoundException.class);
    }

    @Test
    void deleteInquiry_invokesRepo() {
        inquiryService.deleteInquiry(INQUIRY_ID);
        verify(inquiryRepository).deleteById(INQUIRY_ID);
    }
}