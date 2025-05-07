package com.capstone.rentit.locker.repository;

import com.capstone.rentit.config.QuerydslConfig;
import com.capstone.rentit.locker.domain.Locker;
import com.capstone.rentit.locker.dto.LockerSearchForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QuerydslConfig.class)
class CustomLockerRepositoryImplTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private CustomLockerRepositoryImpl lockerRepository;

    // — 헬퍼: 반복되는 저장 로직 분리 —
    private Locker saveLocker(String university, Boolean available) {
        Locker locker = Locker.builder()
                .university(university)
                .available(available)
                .build();
        em.persist(locker);
        em.flush();
        return locker;
    }

    @Test
    @DisplayName("조건 없이 검색하면 모든 사물함을 ID 오름차순으로 반환한다")
    void search_whenNoCriteria_returnsAllSortedById() {
        // given
        Locker a = saveLocker("AAA", true);
        Locker b = saveLocker("BBB", false);
        Locker c = saveLocker("CCC", true);

        LockerSearchForm form = LockerSearchForm.builder().build();

        // when
        List<Locker> results = lockerRepository.search(form);

        // then
        List<Long> ids = results.stream().map(Locker::getLockerId).toList();
        assertThat(ids).containsExactly(a.getLockerId(), b.getLockerId(), c.getLockerId());
    }

    @Test
    @DisplayName("대학명으로 필터링하면 부분일치, 대소문자 무시한다")
    void search_whenUniversityProvided_filtersByUniversityIgnoreCase() {
        // given
        saveLocker("Seoul Univ", true);
        Locker match1 = saveLocker("SEOUL TECH", false);
        saveLocker("Busan Univ", true);

        LockerSearchForm form = LockerSearchForm.builder()
                .university("seoul").build();

        // when
        List<Locker> results = lockerRepository.search(form);

        // then
        assertThat(results)
                .extracting(Locker::getUniversity)
                .allSatisfy(name -> assertThat(name.toLowerCase()).contains("seoul"));
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("available 필터링만 하면 해당 상태의 사물함만 반환한다")
    void search_whenAvailableProvided_filtersByAvailable() {
        // given
        saveLocker("X", true);
        Locker t1 = saveLocker("Y", true);
        saveLocker("Z", false);

        LockerSearchForm form = LockerSearchForm.builder()
                .available(true).build();

        // when
        List<Locker> results = lockerRepository.search(form);

        // then
        assertThat(results)
                .allSatisfy(locker -> assertThat(locker.isAvailable()).isTrue());
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("두 조건을 모두 지정하면 AND 조건으로 동작한다")
    void search_whenBothCriteriaApplied_returnsMatching() {
        // given
        Locker alphaUnivAvailable = saveLocker("Alpha Univ", true);
        saveLocker("Alpha Univ", false);      // available=false 이므로 걸러진다
        Locker alphaExact = saveLocker("ALPHA", true);
        saveLocker("Beta Univ", true);        // university 필터에 걸린다


        LockerSearchForm form = LockerSearchForm.builder()
                .university("alpha").available(true).build();

        // when
        List<Locker> results = lockerRepository.search(form);

        // then
        assertThat(results)
                .extracting(Locker::getLockerId)
                .containsExactly(
                        alphaUnivAvailable.getLockerId(),
                        alphaExact.getLockerId()
                );
        assertThat(results).allSatisfy(locker ->
                assertThat(locker.getUniversity().toLowerCase()).contains("alpha")
        );
        assertThat(results).allSatisfy(locker ->
                assertThat(locker.isAvailable()).isTrue()
        );
    }

    @Test
    @DisplayName("blank 또는 null 대학명은 필터링하지 않는다")
    void search_whenUniversityBlankAndAvailableNull_returnsAll() {
        // given
        Locker a = saveLocker("U1", true);
        Locker b = saveLocker("U2", false);

        LockerSearchForm form = LockerSearchForm.builder()
                .university("  ").available(null).build();

        // when
        List<Locker> results = lockerRepository.search(form);

        // then
        List<Long> ids = results.stream().map(Locker::getLockerId).collect(Collectors.toList());
        assertThat(ids).containsExactly(a.getLockerId(), b.getLockerId());
    }
}