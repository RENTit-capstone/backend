package com.capstone.rentit.dummy;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.item.status.ItemStatusEnum;
import com.capstone.rentit.item.repository.ItemRepository;
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
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Order(2)
public class ItemDummyDataInitializer implements ApplicationRunner {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (itemRepository.count() > 0) {
            return;
        }

        List<Member> owners = memberRepository.findAll();
        if (CollectionUtils.isEmpty(owners)) {
            System.out.println("[ItemDummyDataInitializer] No members found – skip item seeding");
            return;
        }

        // 샘플 아이템 목록 (실제 물건처럼)
        String[] sampleNames = {
                "Samsung Galaxy S21 스마트폰",
                "Dell XPS 13 노트북",
                "Sony WH-1000XM4 무선 헤드폰",
                "Canon EOS M50 미러리스 카메라",
                "TI-84 그래프 계산기",
                "휴대용 보조 배터리 10000mAh",
                "Logitech M330 무선 마우스",
                "Casio EX-word 전자사전",
                "노트북 스탠드",
                "USB-C to HDMI 케이블"
        };
        String[] sampleDescriptions = {
                "최신 모델로, 사용감 거의 없는 상태입니다.",
                "초경량 디자인으로 휴대가 편리합니다.",
                "장시간 착용에도 편안한 무선 헤드폰입니다.",
                "고해상도 사진 촬영이 가능한 미러리스 카메라.",
                "수학·공학 계산에 최적화된 그래프 계산기.",
                "외출 시에도 안심하고 사용할 수 있는 대용량 배터리.",
                "저소음 스위치와 편안한 그립감을 자랑합니다.",
                "다양한 어학 학습 기능을 지원하는 전자사전.",
                "인체공학적 설계로 장시간 사용에도 피로감 최소화.",
                "4K 출력 지원으로 프레젠테이션에 최적화된 케이블."
        };

        final int ITEMS_PER_MEMBER = 5;
        LocalDateTime now = LocalDateTime.now();

        for (Member owner : owners) {
            for (int j = 0; j < ITEMS_PER_MEMBER; j++) {
                // 무작위 샘플 선택
                int idx = ThreadLocalRandom.current().nextInt(sampleNames.length);

                // 하루 대여 요금(원) — 실제 가격대처럼 1,000원~20,000원
                int price = ThreadLocalRandom.current().nextInt(1_000, 20_001);

                Item item = Item.builder()
                        .ownerId(owner.getMemberId())
                        .name(sampleNames[idx])
                        .description(sampleDescriptions[idx])
                        .price(price)
                        .status(randomStatus())
                        .startDate(now.minusDays(ThreadLocalRandom.current().nextInt(0, 3)))
                        .endDate(now.plusDays(ThreadLocalRandom.current().nextInt(1, 7)))
                        .damagedPolicy("분실 또는 파손 시 전액 배상 처리합니다.")
                        .returnPolicy("반납 기한 엄수 부탁드립니다.")
                        .createdAt(now.minusDays(ThreadLocalRandom.current().nextInt(1, 10)))
                        .updatedAt(now)
                        .build();

                itemRepository.save(item);
            }
        }

        System.out.printf("[ItemDummyDataInitializer] %d members × %d realistic items generated.%n",
                owners.size(), ITEMS_PER_MEMBER);
    }

    private ItemStatusEnum randomStatus() {
        ItemStatusEnum[] values = ItemStatusEnum.values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }
}