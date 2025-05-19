package com.capstone.rentit.dummy;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.repository.ItemRepository;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.repository.RentalRepository;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Order(3)
public class RentalDummyDataInitializer implements ApplicationRunner {

    private final RentalRepository rentalRepository;
    private final ItemRepository   itemRepository;
    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (rentalRepository.count() > 0) {
            return;
        }

        List<Item>   items   = itemRepository.findAll();
        List<Member> members = memberRepository.findAll();
        if (CollectionUtils.isEmpty(items) || members.size() < 2) {
            System.out.println("[RentalDummyDataInitializer] Not enough data – skip rental seeding");
            return;
        }

        // owner 제외한 렌터 후보 리스트
        List<Member> rentersAll = members;
        LocalDateTime now = LocalDateTime.now();

        for (Item item : items) {
            Long ownerId = item.getOwnerId();
            List<Member> renters = rentersAll.stream()
                    .filter(m -> !m.getMemberId().equals(ownerId))
                    .collect(Collectors.toList());
            if (renters.isEmpty()) continue;

            // WAITING 을 제외한 상태만 명시
            RentalStatusEnum[] statuses = {
                    RentalStatusEnum.APPROVED,
                    RentalStatusEnum.PICKED_UP,
                    RentalStatusEnum.COMPLETED,
                    RentalStatusEnum.REJECTED
            };

            IntStream.range(0, statuses.length).forEach(i -> {
                RentalStatusEnum status = statuses[i];
                Member renter = renters.get(i % renters.size());

                // 날짜를 i+1 일 전으로 고정
                LocalDateTime requestDate = now.minusDays(i + 1L);
                LocalDateTime startDate   = requestDate.plusDays(1);
                LocalDateTime dueDate     = startDate.plusDays(7);

                Rental.RentalBuilder b = Rental.builder()
                        .itemId(item.getItemId())
                        .ownerId(ownerId)
                        .renterId(renter.getMemberId())
                        .requestDate(requestDate)
                        .startDate(startDate)
                        .dueDate(dueDate)
                        .status(status)
                        // 공통 필드는 기본 null
                        .approvedDate(null)
                        .rejectedDate(null)
                        .leftAt(null)
                        .pickedUpAt(null)
                        .returnedAt(null)
                        .retrievedAt(null)
                        .deviceId(null)
                        .lockerId(null)
                        .returnImageUrl(null);

                // 상태별 고정된 타임스탬프 세팅
                switch (status) {
                    case APPROVED -> b.approvedDate(requestDate.plusHours(2));
                    case PICKED_UP -> b
                            .approvedDate(requestDate.plusHours(2))
                            .leftAt(startDate)
                            .pickedUpAt(startDate.plusHours(1));
                    case COMPLETED -> b
                            .approvedDate(requestDate.plusHours(2))
                            .leftAt(startDate)
                            .pickedUpAt(startDate.plusHours(1))
                            .returnedAt(dueDate.minusDays(1))
                            .retrievedAt(dueDate.minusDays(1).plusHours(3))
                            .returnImageUrl("dummy/return-image.jpg");
                    case REJECTED -> b.rejectedDate(requestDate.plusHours(1));
                    default -> {}
                }

                rentalRepository.save(b.build());
            });
        }
    }
}
