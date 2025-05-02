package com.capstone.rentit.dummy;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.item.repository.ItemRepository;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class ItemDummyDataInitializer implements ApplicationRunner {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 이미 아이템이 있으면 스킵
        if (itemRepository.count() > 0) {
            return;
        }

        List<Member> owners = memberRepository.findAll();
        if (CollectionUtils.isEmpty(owners)) {
            System.out.println("[ItemDummyDataInitializer] No members found – skip item seeding");
            return;
        }

        // 생성할 더미 아이템 수
        final int ITEMS_PER_MEMBER = 5;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < owners.size(); i++) {
            Member owner = owners.get(i);

            for (int j = 1; j <= ITEMS_PER_MEMBER; j++) {
                Item item = Item.builder()
                        .ownerId(owner.getMemberId())
                        .name(String.format("%s's Item #%d", owner.getName(), j))
                        .description("이 물품은 더미 데이터로 생성되었습니다.")
                        .price(ThreadLocalRandom.current().nextInt(500, 5001))       // 500원 ~ 5000원
                        .status(randomStatus())
                        .startDate(now.minusDays(ThreadLocalRandom.current().nextInt(0, 3)))
                        .endDate(now.plusDays(ThreadLocalRandom.current().nextInt(1, 7)))
                        .damagedPolicy("분실 또는 파손 시 전액 배상합니다.")
                        .returnPolicy("반납 기한 엄수 요망.")
                        .createdAt(now.minusDays(ThreadLocalRandom.current().nextInt(1, 10)))
                        .updatedAt(now)
                        .build();

                itemRepository.save(item);
            }
        }

        System.out.printf("[ItemDummyDataInitializer] %d members × %d items generated.%n",
                owners.size(), ITEMS_PER_MEMBER);
    }

    private ItemStatusEnum randomStatus() {
        ItemStatusEnum[] values = ItemStatusEnum.values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }
}
