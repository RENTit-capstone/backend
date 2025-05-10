package com.capstone.rentit.inquiry.service;

import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.inquiry.dto.InquiryCreateForm;
import com.capstone.rentit.inquiry.dto.InquiryResponse;
import com.capstone.rentit.inquiry.dto.InquirySearchForm;
import com.capstone.rentit.inquiry.exception.InquiryNotFoundException;
import com.capstone.rentit.inquiry.repository.InquiryRepository;
import com.capstone.rentit.inquiry.type.InquiryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @InjectMocks
    private InquiryService inquiryService;

    private Inquiry sampleInquiry;

    @BeforeEach
    void setUp() {
        sampleInquiry = Inquiry.builder()
                .inquiryId(42L)
                .memberId(100L)
                .type(InquiryType.SERVICE)
                .title("Help")
                .content("Need assistance")
                .processed(false)
                .createdAt(LocalDateTime.of(2025, 5, 10, 12, 0))
                .build();
    }

    @Test
    @DisplayName("createInquiry: 저장된 엔티티 ID를 반환하고, repository.save 호출")
    void createInquiry_success() {
        var form = new InquiryCreateForm(
                100L, "Help", "Need assistance", InquiryType.SERVICE
        );

        when(inquiryRepository.save(any(Inquiry.class)))
                .thenAnswer(invocation -> {
                    Inquiry arg = invocation.getArgument(0);
                    return Inquiry.builder()
                            .inquiryId(1L)
                            .memberId(arg.getMemberId())
                            .type(arg.getType())
                            .title(arg.getTitle())
                            .content(arg.getContent())
                            .processed(arg.isProcessed())
                            .createdAt(arg.getCreatedAt())
                            .build();
                });

        Long returnedId = inquiryService.createInquiry(form);

        assertThat(returnedId).isEqualTo(1L);

        ArgumentCaptor<Inquiry> captor = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryRepository, times(1)).save(captor.capture());
        Inquiry saved = captor.getValue();

        assertThat(saved.getMemberId()).isEqualTo(100L);
        assertThat(saved.getType()).isEqualTo(InquiryType.SERVICE);
        assertThat(saved.getTitle()).isEqualTo("Help");
        assertThat(saved.getContent()).isEqualTo("Need assistance");
        assertThat(saved.isProcessed()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getInquiry: 존재하는 ID 조회 시 올바른 DTO 반환")
    void getInquiry_found() {
        when(inquiryRepository.findById(42L))
                .thenReturn(Optional.of(sampleInquiry));

        InquiryResponse resp = inquiryService.getInquiry(42L);

        assertThat(resp.inquiryId()).isEqualTo(42L);
        assertThat(resp.memberId()).isEqualTo(100L);
        assertThat(resp.type()).isEqualTo(InquiryType.SERVICE);
        assertThat(resp.title()).isEqualTo("Help");
        assertThat(resp.content()).isEqualTo("Need assistance");
        assertThat(resp.processed()).isFalse();
        assertThat(resp.createdAt()).isEqualTo(sampleInquiry.getCreatedAt());
    }

    @Test
    @DisplayName("getInquiry: 없는 ID 조회 시 InquiryNotFoundException 발생")
    void getInquiry_notFound() {
        when(inquiryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.getInquiry(99L))
                .isInstanceOf(InquiryNotFoundException.class)
                .hasMessageContaining("존재하지 않는 문의 ID");
    }

    @Test
    @DisplayName("getInquiries: memberId 로 전체 조회 (type null)")
    void getInquiries_allTypes() {
        Inquiry other = Inquiry.builder()
                .inquiryId(43L)
                .memberId(100L)
                .type(InquiryType.REPORT)
                .title("Report")
                .content("Issue")
                .build();

        when(inquiryRepository.findByMemberId(100L))
                .thenReturn(List.of(sampleInquiry, other));

        var list = inquiryService.getInquiries(100L, null);

        assertThat(list).hasSize(2)
                .extracting(InquiryResponse::type)
                .containsExactlyInAnyOrder(InquiryType.SERVICE, InquiryType.REPORT);
    }

    @Test
    @DisplayName("getInquiries: memberId + type 필터 조회")
    void getInquiries_byType() {
        when(inquiryRepository.findByMemberId(100L))
                .thenReturn(List.of(sampleInquiry));

        var list = inquiryService.getInquiries(100L, InquiryType.SERVICE);

        assertThat(list).hasSize(1)
                .allMatch(r -> r.type() == InquiryType.SERVICE);
    }

    @Test
    @DisplayName("search: repository.search 호출 후 DTO 매핑")
    void search_delegationAndMapping() {
        var form = new InquirySearchForm(
                InquiryType.SERVICE, null, null, null
        );
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Inquiry> stubPage = new PageImpl<>(
                List.of(sampleInquiry), pageable, 1L
        );
        when(inquiryRepository.search(form, pageable))
                .thenReturn(stubPage);

        Page<InquiryResponse> result = inquiryService.search(form, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1)
                .first()
                .satisfies(r -> {
                    assertThat(r.inquiryId()).isEqualTo(42L);
                    assertThat(r.type()).isEqualTo(InquiryType.SERVICE);
                });
        verify(inquiryRepository).search(form, pageable);
    }

    @Test
    @DisplayName("deleteInquiry: repository.deleteById 호출")
    void deleteInquiry_invokesRepository() {
        inquiryService.deleteInquiry(77L);
        verify(inquiryRepository, times(1)).deleteById(77L);
    }

    @Test
    @DisplayName("markProcessed: 기존 inquiry 의 processed 가 true 로 변경")
    void markProcessed_success() {
        when(inquiryRepository.findById(42L))
                .thenReturn(Optional.of(sampleInquiry));

        inquiryService.markProcessed(42L);

        assertThat(sampleInquiry.isProcessed()).isTrue();
    }

    @Test
    @DisplayName("markProcessed: 없는 ID 처리 시 InquiryNotFoundException")
    void markProcessed_notFound() {
        when(inquiryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.markProcessed(99L))
                .isInstanceOf(InquiryNotFoundException.class);
    }
}