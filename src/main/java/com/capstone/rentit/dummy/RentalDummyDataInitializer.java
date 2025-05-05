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
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class RentalDummyDataInitializer implements ApplicationRunner {

    private final RentalRepository rentalRepository;
    private final ItemRepository   itemRepository;
    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 이미 렌털 데이터가 있으면 스킵
        if (rentalRepository.count() > 0) {
            return;
        }

        List<Item>   items   = itemRepository.findAll();
        List<Member> members = memberRepository.findAll();

        if (CollectionUtils.isEmpty(items) || members.size() < 2) {
            System.out.println("[RentalDummyDataInitializer] Not enough data – skip rental seeding");
            return;
        }

        /* ----------------------------------------------------------
           생성 규칙
           • 각 Item 마다 3개의 Rental 을 만든다
           • renter 는 owner 와 다른 Member 중 무작위
        ---------------------------------------------------------- */
        final int RENTALS_PER_ITEM = 3;
        LocalDateTime now = LocalDateTime.now();

        for (Item item : items) {
            Long ownerId = item.getOwnerId();

            for (int i = 0; i < RENTALS_PER_ITEM; i++) {
                Member renter = randomRenter(members, ownerId);

                LocalDateTime requestDate = now.minusDays(rand(1, 7));
                LocalDateTime startDate   = requestDate.plusDays(1);
                LocalDateTime dueDate     = startDate.plusDays(rand(3, 14));

                RentalStatusEnum status = randomStatus();

                Rental.RentalBuilder builder = Rental.builder()
                        .itemId(item.getItemId())
                        .ownerId(ownerId)
                        .renterId(renter.getMemberId())
                        .requestDate(requestDate)
                        .startDate(startDate)
                        .dueDate(dueDate)
                        .status(status)
                        /* 공통 필드 기본값 */
                        .approvedDate(null)
                        .rejectedDate(null)
                        .leftAt(null)
                        .pickedUpAt(null)
                        .returnedAt(null)
                        .retrievedAt(null)
                        .lockerId(null)
                        .paymentId(null)
                        .returnImageUrl(null);

                /* 상태별 날짜 세팅 */
                switch (status) {
                    case APPROVED -> builder
                            .approvedDate(requestDate.plusHours(rand(1, 12)));
                    case PICKED_UP -> builder
                            .approvedDate(requestDate.plusHours(2))
                            .leftAt(startDate)
                            .pickedUpAt(startDate.plusHours(1));
                    case COMPLETED -> builder
                            .approvedDate(requestDate.plusHours(2))
                            .leftAt(startDate)
                            .pickedUpAt(startDate.plusHours(1))
                            .returnedAt(dueDate.minusDays(1))
                            .retrievedAt(dueDate.minusDays(1).plusHours(3))
                            .returnImageUrl("dummy/return-image.jpg");
                    case REJECTED -> builder
                            .rejectedDate(requestDate.plusHours(rand(1, 6)));
                    default -> { /* WAITING → 추가 필드 없음 */ }
                }

                rentalRepository.save(builder.build());
            }
        }

        System.out.printf("[RentalDummyDataInitializer] %d items × %d rentals generated.%n",
                items.size(), RENTALS_PER_ITEM);
    }

    private int rand(int minInclusive, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    private RentalStatusEnum randomStatus() {
        RentalStatusEnum[] values = RentalStatusEnum.values();
        return values[rand(0, values.length - 1)];
    }

    /** owner 와 다른 member 중 무작위 선택 */
    private Member randomRenter(List<Member> members, Long ownerId) {
        Member renter;
        do {
            renter = members.get(rand(0, members.size() - 1));
        } while (renter.getMemberId().equals(ownerId));
        return renter;
    }
}
