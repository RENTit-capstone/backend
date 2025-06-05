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
                "접이식 우산",
                "삼성 13인치 노트북",
                "보스 노이즈캔슬링 헤드폰",
                "캐논 EOS M50 미러리스 카메라",
                "TI-84 그래프 계산기",
                "샤오미 10000mAh 보조 배터리",
                "로지텍 휴대용 무소음 마우스",
                "킨들 페이퍼화이트 전자책 리더",
                "고프로 히어로9 액션 카메라",
                "애플 아이패드 10.2",
                "JBL Flip 6 블루투스 스피커",
                "닌텐도 스위치 라이트",
                "시게이트 2TB 외장 SSD",
                "와콤 인튜어스 S 드로잉 태블릿",
                "DJI 오즈모 모바일 6 짐벌"
        };
        String[] desc = {
                "비 오는 날 간편하게 휴대 가능한 소형 접이식 우산입니다.",
                "휴대성과 성능을 겸비한 삼성 13인치 노트북입니다.",
                "강력한 노이즈 캔슬링 기능을 제공하는 무선 헤드폰입니다.",
                "풀 HD 동영상 촬영이 가능한 입문용 미러리스 카메라입니다.",
                "복잡한 수학 계산과 그래프 기능을 지원하는 그래프 계산기입니다.",
                "휴대폰 충전을 위한 고용량 10000mAh 보조 배터리입니다.",
                "저소음 클릭으로 도서관 등 환경에서도 사용하기 좋은 무선 마우스입니다.",
                "야간 독서에도 적합한 전자잉크 기반의 전자책 리더기입니다.",
                "4K 영상 촬영과 방수 기능을 제공하는 액션 카메라입니다.",
                "교육용 및 멀티미디어 감상에 적합한 10.2인치 태블릿입니다.",
                "강력한 베이스와 휴대성을 갖춘 방수 블루투스 스피커입니다.",
                "휴대용으로 최적화된 게임 플레이가 가능한 닌텐도 스위치 라이트입니다.",
                "빠른 데이터 전송 속도를 자랑하는 2TB 외장 SSD입니다.",
                "초보자용에 적합한 라이트 스타일의 드로잉 태블릿입니다.",
                "스마트폰 촬영 시 안정적인 영상을 위한 3축 짐벌입니다."
        };

        ItemStatusEnum[] statuses = ItemStatusEnum.values();
        final int ITEMS_PER_MEMBER = 3;
        LocalDateTime now = LocalDateTime.now();

        int global = 0;
        for (Member owner : owners) {
            for (int j = 0; j < ITEMS_PER_MEMBER; j++, global++) {
                int i     = global % names.length;
                int price = 1_000 * (i + 1);               // 1,000원 단위
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