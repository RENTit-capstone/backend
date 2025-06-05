package com.capstone.rentit.dummy;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.repository.ItemRepository;
import com.capstone.rentit.item.status.ItemStatusEnum;
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

@Component
@RequiredArgsConstructor
@Order(3)
public class RentalDummyDataInitializer implements ApplicationRunner {

    private final RentalRepository rentalRepository;
    private final ItemRepository   itemRepository;
    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) {
//        if (rentalRepository.count() > 0) return;

        // STUDENT 역할의 회원만 필터링
        List<Member> members = memberRepository.findAll().stream()
                .filter(m -> m.getRole() == MemberRoleEnum.STUDENT)
                .toList();

        if (members.size() < 2) {
            System.out.println("[RentalDummy] 학생이 2명 이상 필요 – 스킵");
            return;
        }

        // 상태가 OUT(대여 가능한)인 아이템만 필터링
        List<Item> outItems = itemRepository.findAll().stream()
                .filter(it -> it.getStatus() == ItemStatusEnum.OUT)
                .toList();

        if (outItems.size() < 2) {
            System.out.println("[RentalDummy] OUT 상태인 아이템이 2개 이상 필요 – 스킵");
            return;
        }

        // 두 명의 학생을 고정
        Member renter1 = members.get(0);
        Member renter2 = members.get(1);

        // renter1 을 위한 아이템 찾기 (소유주가 다른 OUT 아이템)
        Item itemForRenter1 = outItems.stream()
                .filter(it -> !it.getOwnerId().equals(renter1.getMemberId()))
                .findFirst()
                .orElse(null);

        // renter2 을 위한 아이템 찾기 (소유주가 다른 OUT 아이템, renter1 과 중복되지 않도록)
        Item itemForRenter2 = outItems.stream()
                .filter(it -> !it.getOwnerId().equals(renter2.getMemberId())
                        && (itemForRenter1 == null || !it.getItemId().equals(itemForRenter1.getItemId())))
                .findFirst()
                .orElse(null);

        if (itemForRenter1 == null || itemForRenter2 == null) {
            System.out.println("[RentalDummy] 각 학생에게 배정할 수 있는 OUT 아이템이 부족 – 스킵");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        /* 1. 첫 번째 학생(renter1)에게 APPROVED 상태의 대여 하나 생성 */
        {
            LocalDateTime requestDate = now.minusDays(2);
            LocalDateTime startDate   = requestDate.plusDays(1);
            LocalDateTime dueDate     = startDate.plusDays(7);

            Rental rental1 = Rental.builder()
                    .itemId(itemForRenter1.getItemId())
                    .ownerId(itemForRenter1.getOwnerId())
                    .renterId(renter1.getMemberId())
                    .requestDate(requestDate)
                    .startDate(startDate)
                    .dueDate(dueDate)
                    .status(RentalStatusEnum.APPROVED)
                    .approvedDate(requestDate.plusHours(3))
                    .build();

            rentalRepository.save(rental1);
        }

        /* 2. 두 번째 학생(renter2)에게 COMPLETED 상태의 대여 하나 생성 */
        {
            LocalDateTime requestDate = now.minusDays(10);
            LocalDateTime startDate   = requestDate.plusDays(1);
            LocalDateTime dueDate     = startDate.plusDays(7);
            LocalDateTime returnedAt  = dueDate.minusDays(1);
            LocalDateTime retrievedAt = returnedAt.plusHours(2);

            Rental rental2 = Rental.builder()
                    .itemId(itemForRenter2.getItemId())
                    .ownerId(itemForRenter2.getOwnerId())
                    .renterId(renter2.getMemberId())
                    .requestDate(requestDate)
                    .startDate(startDate)
                    .dueDate(dueDate)
                    .status(RentalStatusEnum.COMPLETED)
                    .approvedDate(requestDate.plusHours(2))
                    .leftAt(startDate)
                    .pickedUpAt(startDate.plusHours(1))
                    .returnedAt(returnedAt)
                    .retrievedAt(retrievedAt)
                    .returnImageUrl("dummy/return_completed.jpg")
                    .build();

            rentalRepository.save(rental2);
        }

        System.out.println("[RentalDummy] 2명의 학생에게 고정된 대여 데이터 생성 완료.");
    }
}