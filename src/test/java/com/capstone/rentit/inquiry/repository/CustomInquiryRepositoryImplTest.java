package com.capstone.rentit.inquiry.repository;

import com.capstone.rentit.config.QuerydslConfig;
import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.inquiry.dto.InquirySearchForm;
import com.capstone.rentit.inquiry.type.InquiryType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({QuerydslConfig.class})
class CustomInquiryRepositoryImplTest {
    @Autowired
    private CustomInquiryRepository inquiryRepository;

    @Autowired
    private EntityManager em;

    private final LocalDateTime baseTime = LocalDateTime.now().minusDays(1);

    @BeforeEach
    void setUp() {
        // --- 테스트 픽스처: 10건(서비스 6, 신고 4) --- //
        persist(InquiryType.SERVICE, false, 6);
        persist(InquiryType.REPORT,  true, 2);
        persist(InquiryType.REPORT,  false,2);
    }

    /* -------- 단일 조건 & 페이징 조합 검증 -------- */
    @ParameterizedTest
    @CsvSource({
            // type, processed, expectedTotal
            "SERVICE, , 6",     // processed 파라미터 null
            "REPORT,  true, 2",
            "REPORT,  false,2"
    })
    @DisplayName("검색 조건(type + processed)과 totalCount 검증")
    void search_byTypeAndProcessed(InquiryType type, Boolean processed, long expected) {

        InquirySearchForm form = InquirySearchForm.builder()
                .type(type)
                .processed(processed)
                .build();

        Pageable pageable = PageRequest.of(0, 5, Sort.Direction.DESC, "createdAt");

        var page = inquiryRepository.search(form, pageable);

        // total count
        assertThat(page.getTotalElements()).isEqualTo(expected);

        // 페이징: 첫 페이지면 size <= 5
        assertThat(page.getContent().size())
                .isBetween(0, 5);

        // 결과 타입 일치
        assertThat(page.getContent())
                .allMatch(i -> i.getType() == type);

        // processed 필터 일치 (null 이면 스킵)
        if (processed != null) {
            assertThat(page.getContent())
                    .allMatch(i -> i.isProcessed() == processed);
        }
    }

    /* -------- 날짜 범위 검증 -------- */
    @DisplayName("fromDate ~ toDate 범위 조건을 만족해야 한다")
    @Test
    void search_byDateRange() {

        LocalDateTime from = baseTime.plusHours(2);   // 일부만 포함
        LocalDateTime to   = baseTime.plusHours(4);

        InquirySearchForm form = InquirySearchForm.builder()
                .type(InquiryType.SERVICE)
                .fromDate(from)
                .toDate(to)
                .build();

        Page<Inquiry> page = inquiryRepository.search(form, Pageable.unpaged());

        // SERVICE 6건 중 3건만 해당 시간대에 저장되도록 persist()
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent())
                .allMatch(i ->
                        !i.getCreatedAt().isBefore(from) &&
                                !i.getCreatedAt().isAfter(to));
    }

    // ====== 헬퍼 ======
    private void persist(InquiryType type, boolean processed, int count) {
        for (int i = 0; i < count; i++) {
            Inquiry entity = Inquiry.builder()
                    .memberId(1L)
                    .type(type)
                    .title(type + "‑title" + i)
                    .content("dummy")
                    .processed(processed)
                    .createdAt(baseTime.plusHours(i)) // 시간대 분포
                    .build();
            em.persist(entity);
        }
        em.flush();
    }
}