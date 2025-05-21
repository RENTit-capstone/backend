package com.capstone.rentit.dummy;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.status.ItemStatusEnum;   // ★ 추가
import com.capstone.rentit.item.repository.ItemRepository;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.repository.RentalRepository;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Order(3)
public class RentalDummyDataInitializer implements ApplicationRunner {

    private final RentalRepository rentalRepository;
    private final ItemRepository   itemRepository;
    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (rentalRepository.count() > 0) return;

        List<Member> members = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == MemberRoleEnum.STUDENT).toList();
        /* 상태가 OUT 인 아이템만 필터링 */
        List<Item> outItems = itemRepository.findAll().stream()
                .filter(it -> it.getStatus() == ItemStatusEnum.OUT)
                .toList();

        if (outItems.isEmpty()) {
            System.out.println("[RentalDummy] Need ≥5 members and at least one OUT-item – skip");
            return;
        }

        RentalStatusEnum[] states = {
                RentalStatusEnum.APPROVED,
                RentalStatusEnum.PICKED_UP,
                RentalStatusEnum.COMPLETED,
                RentalStatusEnum.REJECTED
        };
        LocalDateTime now = LocalDateTime.now();

        for (Member renter : members) {
            /* 자신의 OUT-아이템은 제외 */
            List<Item> candidates = outItems.stream()
                    .filter(it -> !it.getOwnerId().equals(renter.getMemberId()))
                    .collect(Collectors.toList());
            if (candidates.isEmpty()) continue;

            for (int k = 0; k < states.length; k++) {
                Item item = candidates.get(k % candidates.size());
                RentalStatusEnum st = states[k];

                LocalDateTime req = now.minusDays(k + 1);
                LocalDateTime start = req.plusDays(1);
                LocalDateTime due = start.plusDays(7);

                Rental.RentalBuilder b = Rental.builder()
                        .itemId(item.getItemId())
                        .ownerId(item.getOwnerId())
                        .renterId(renter.getMemberId())
                        .requestDate(req)
                        .startDate(start)
                        .dueDate(due)
                        .status(st);

                switch (st) {
                    case APPROVED -> b.approvedDate(req.plusHours(2));
                    case PICKED_UP -> b.approvedDate(req.plusHours(2))
                            .leftAt(start)
                            .pickedUpAt(start.plusHours(1));
                    case COMPLETED -> b.approvedDate(req.plusHours(2))
                            .leftAt(start)
                            .pickedUpAt(start.plusHours(1))
                            .returnedAt(due.minusDays(1))
                            .retrievedAt(due.minusDays(1).plusHours(3))
                            .returnImageUrl("dummy/return.jpg");
                    case REJECTED -> b.rejectedDate(req.plusHours(1));
                    default -> {}
                }
                rentalRepository.save(b.build());
            }
        }
        System.out.printf("[RentalDummy] %d members × 4 rentals seeded (OUT items only).%n",
                members.size());
    }
}
