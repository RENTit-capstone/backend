package com.capstone.rentit.dummy;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.repository.ItemRepository;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(2)
public class ItemDummyDataInitializer implements ApplicationRunner {

    private final ItemRepository   itemRepository;
    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) {
//        if (itemRepository.count() > 0) return;

        List<Member> owners = memberRepository.findAll();
        if (CollectionUtils.isEmpty(owners)) {
            System.out.println("[ItemDummy] Exactly 5 members required – skip item seeding");
            return;
        }

        /* 15 개의 고정 샘플 (5명 × 3개) */
        String[] names = {
                "접이식 우산", "접이식 우산", "접이식 우산",
                "접이식 우산", "접이식 우산", "접이식 우산",
                "접이식 우산", "접이식 우산", "접이식 우산",
                "Apple iPad 10.2", "JBL Flip 6", "Nintendo Switch Lite",
                "Seagate 2TB SSD", "Wacom Intuos S", "DJI Osmo Mobile 6"
        };
        String[] desc = {
                "최신 안드로이드 스마트폰.", "초경량 13인치 노트북.", "노캔 무선 헤드폰.",
                "입문용 미러리스 카메라.", "그래프 계산기.", "10000mAh 보조 배터리.",
                "저소음 휴대용 마우스.", "전자잉크 e-리더기.", "액션 카메라.",
                "가성비 태블릿.", "휴대용 블루투스 스피커.", "휴대용 게임기.",
                "외장 SSD 2TB.", "드로잉 태블릿.", "스마트폰 짐벌."
        };

        ItemStatusEnum[] statuses = ItemStatusEnum.values();
        final int ITEMS_PER_MEMBER = 3;
        LocalDateTime now = LocalDateTime.now();

        int global = 0;
        for (Member owner : owners) {
            for (int j = 0; j < ITEMS_PER_MEMBER; j++, global++) {
                int i     = global % names.length;
                int price = 1_000 * (i + 1);               // 1 000 원 단위
                ItemStatusEnum st = statuses[global % statuses.length];

                itemRepository.save(Item.builder()
                        .ownerId(owner.getMemberId())
                        .name(names[i])
                        .description(desc[i])
                        .price(price)
                        .status(st)
                        .startDate(now.minusDays(global + 1))
                        .endDate(now.plusDays(global + 10))
                        .damagedPolicy("분실·파손 시 전액 배상.")
                        .returnPolicy("반납 기한 준수 필수.")
                        .createdAt(now.minusDays(global))
                        .updatedAt(now)
                        .build());
            }
        }
        System.out.printf("[ItemDummy] %d items seeded.%n",
                owners.size() * ITEMS_PER_MEMBER);
    }
}
