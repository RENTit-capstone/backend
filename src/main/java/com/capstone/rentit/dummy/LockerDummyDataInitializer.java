package com.capstone.rentit.dummy;

import com.capstone.rentit.locker.domain.Locker;
import com.capstone.rentit.locker.repository.LockerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Order(4)
public class LockerDummyDataInitializer implements ApplicationRunner {

    private final LockerRepository lockerRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 이미 존재하면 스킵
        if (lockerRepository.count() > 0) {
            return;
        }

        /* 더미 대학 목록 ‑ 필요시 수정 */
        List<String> universities = List.of(
                "서울대학교", "고려대학교", "연세대학교",
                "성균관대학교", "한양대학교"
        );

        if (CollectionUtils.isEmpty(universities)) return;

        final int LOCKERS_PER_UNIV = 10;
        LocalDateTime now = LocalDateTime.now();

        for (String univ : universities) {
            for (int i = 1; i <= LOCKERS_PER_UNIV; i++) {

                boolean available = ThreadLocalRandom.current().nextBoolean(); // 50% 가용

                Locker locker = Locker.builder()
                        .available(available)
                        .university(univ)
                        .locationDescription(
                                String.format("%s 학생회관 앞 %02d번", univ, i)
                        )
                        .activatedAt(
                                available
                                        ? now.minusDays(ThreadLocalRandom.current()
                                        .nextInt(0, 30))
                                        : null          // 비가용이면 활성화 날짜 없음
                        )
                        .build();

                lockerRepository.save(locker);
            }
        }

        System.out.printf("[LockerDummyDataInitializer] %d universities × %d lockers generated.%n",
                universities.size(), LOCKERS_PER_UNIV);
    }
}

