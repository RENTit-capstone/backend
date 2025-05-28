package com.capstone.rentit.inquiry.repository;

import com.capstone.rentit.config.QuerydslConfig;
import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.inquiry.dto.InquirySearchForm;
import com.capstone.rentit.inquiry.type.InquiryType;
import com.capstone.rentit.member.status.MemberRoleEnum;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
@ActiveProfiles("test")
@DisplayName("CustomInquiryRepositoryImpl 통합 테스트")
class CustomInquiryRepositoryImplTest {

    @Autowired EntityManager em;
    @Autowired CustomInquiryRepository inquiryRepository;

    private final LocalDateTime base = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

    @BeforeEach
    void setUp() {
        // A가 작성한 SERVICE 문의 3건 (processed true 1 / false 2)
        persistBulk(1L, null, InquiryType.SERVICE, true,  1);
        persistBulk(1L, null, InquiryType.SERVICE, false, 2);

        // B가 작성 – A가 상대(target)인 DAMAGE 신고 2건
        persistBulk(2L, 1L, InquiryType.DAMAGE,  false, 2);

        // 그 외 B가 작성한 REPORT 1건
        persistBulk(2L, null, InquiryType.REPORT, false, 1);
    }

    @Nested
    @DisplayName("역할(Role) 필터링")
    class RoleFilter {

        @Test @DisplayName("ADMIN 은 모든 문의를 볼 수 있다")
        void adminSeesAll() {

            InquirySearchForm form = InquirySearchForm.builder().build();
            Page<Inquiry> page = inquiryRepository.search(
                    form, MemberRoleEnum.ADMIN, null, Pageable.unpaged());

            assertThat(page.getTotalElements()).isEqualTo(6);
        }

        @Test @DisplayName("STUDENT 는 본인이 작성했거나 피신고된 문의만 본다")
        void studentSeesOwnAndTarget() {

            InquirySearchForm form = InquirySearchForm.builder().build();
            Page<Inquiry> page = inquiryRepository.search(
                    form, MemberRoleEnum.STUDENT, 1L, Pageable.unpaged());

            // A가 볼 수 있는 것은 1) 자신이 쓴 SERVICE 3건 + 2) 대상이 자신인 DAMAGE 2건 = 5
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getContent())
                    .allSatisfy(inq ->
                            assertThat(
                                    inq.getMemberId().equals(1L)   // 내가 작성
                                            || Long.valueOf(1L).equals(inq.getTargetMemberId()) // 혹은 피신고자
                            ).isTrue()
                    );
        }
    }

    @ParameterizedTest(name = "[{index}] type={0} processed={1} ⇒ expected={2}")
    @CsvSource({
            "SERVICE, ,    3",
            "DAMAGE,  false,2",
            "REPORT,  false,1"
    })
    @DisplayName("type + processed 조건과 totalCount 검증 (ADMIN)")
    void filterByTypeAndProcessed(InquiryType type, Boolean processed, long expected) {

        InquirySearchForm form = InquirySearchForm.builder()
                .type(type)
                .processed(processed)
                .build();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        Page<Inquiry> page = inquiryRepository.search(form, MemberRoleEnum.ADMIN, null, pageable);

        assertThat(page.getTotalElements()).isEqualTo(expected);
        assertThat(page.getContent()).allMatch(inq -> inq.getType() == type);

        if (processed != null) {
            assertThat(page.getContent()).allMatch(inq -> inq.isProcessed() == processed);
        }
    }

    @Test @DisplayName("fromDate ~ toDate 조건이 정확히 적용된다")
    void filterByDateRange() {

        LocalDateTime from = base.plusHours(1);      // 첫 SERVICE 이후
        LocalDateTime to   = base.plusHours(2);      // 두 번째 SERVICE 까지

        InquirySearchForm form = InquirySearchForm.builder()
                .fromDate(from)
                .toDate(to)
                .build();

        Page<Inquiry> page = inquiryRepository.search(
                form, MemberRoleEnum.ADMIN, null, Pageable.unpaged());

        assertThat(page.getTotalElements()).isEqualTo(2); // SERVICE 두 건
        assertThat(page.getContent())
                .allMatch(inq ->
                        !inq.getCreatedAt().isBefore(from)
                                && !inq.getCreatedAt().isAfter(to));
    }

    @Test @DisplayName("page / size 파라미터에 따라 content 개수가 제한된다")
    void pagingWorks() {

        Pageable pageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());

        Page<Inquiry> page = inquiryRepository.search(
                InquirySearchForm.builder().type(InquiryType.SERVICE).build(),
                MemberRoleEnum.ADMIN, null, pageable);

        assertThat(page.getContent().size()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(3);   // SERVICE 전체는 3
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    private void persistBulk(Long writerId,
                             Long targetId,
                             InquiryType type,
                             boolean processed,
                             int count) {

        for (int i = 0; i < count; i++) {
            Inquiry entity = Inquiry.builder()
                    .memberId(writerId)
                    .targetMemberId(targetId)
                    .type(type)
                    .title("%s-title-%d".formatted(type, i))
                    .content("dummy")
                    .processed(processed)
                    .createdAt(base.plusHours(i))
                    .build();
            em.persist(entity);
        }
        em.flush();
        em.clear();
    }
}
