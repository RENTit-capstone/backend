package com.capstone.rentit.dummy;

import com.capstone.rentit.locker.domain.Device;
import com.capstone.rentit.locker.domain.Locker;
import com.capstone.rentit.locker.repository.DeviceRepository;
import com.capstone.rentit.locker.repository.LockerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Order(4)
public class DeviceLockerDummyDataInitializer implements ApplicationRunner {

    private final DeviceRepository deviceRepository;
    private final LockerRepository lockerRepository;

    @Override
    public void run(ApplicationArguments args) {
//        if (deviceRepository.count() > 0) {
//            return;
//        }

        // 더미 대학 및 위치 설명 맵
        List<String> universities = List.of("아주대학교");
        Map<String, String> locations = Map.of(
                "아주대학교-1", "팔달관 1층 복도",
                "아주대학교-2", "중앙도서관 1층 복도"
        );

        if (CollectionUtils.isEmpty(universities)) {
            return;
        }

        final int LOCKERS_PER_DEVICE = 10;
        LocalDateTime now = LocalDateTime.now();

        for (String univ : universities) {
            // 1) Device 생성 & 저장
            Device device = Device.builder()
                    .university(univ)
                    .locationDescription(locations.getOrDefault(univ, "캠퍼스 내부"))
                    .build();
            deviceRepository.save(device);

            // 2) 각 Device에 Locker 생성 & 저장
            for (int num = 1; num <= LOCKERS_PER_DEVICE; num++) {
                boolean available = ThreadLocalRandom.current().nextBoolean();
                LocalDateTime activatedAt = available
                        ? now.minusDays(ThreadLocalRandom.current().nextInt(0, 30))
                        : null;  // 가용(false)이면 활성화 날짜 없음

                Locker locker = Locker.builder()
                        .deviceId(device.getDeviceId())
                        .lockerId((long) num)
                        .available(available)
                        .activatedAt(activatedAt)
                        .build();
                lockerRepository.save(locker);
            }
        }

        System.out.printf("[DeviceLockerDummyDataInitializer] %d devices × %d lockers each generated.%n",
                universities.size(), LOCKERS_PER_DEVICE);
    }
}
