package com.capstone.rentit.dummy;

import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.inquiry.type.InquiryType;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(7)
public class InquiryDummyDataInitializer implements ApplicationRunner {

    private final InquiryRepository  inquiryRepository;
    private final MemberRepository   memberRepository;

    @Override
    public void run(ApplicationArguments args) {
//        if (inquiryRepository.count() > 0) return;

        List<Member> members = memberRepository.findAll();
        if (members.size() < 2) {
            System.out.println("[InquiryDummy] 회원이 2명 이상 필요 – 스킵");
            return;
        }

        Member memberA = members.get(0);
        Member memberB = members.get(1);

        LocalDateTime now = LocalDateTime.now();

        /* 1. 첫 번째 회원에게 SERVICE 문의 */
        Inquiry serviceInquiry = Inquiry.builder()
                .memberId(memberA.getMemberId())
                .type(InquiryType.SERVICE)
                .title("앱 로그인 관련 문의")
                .content("앱에 로그인이 되지 않습니다. 아이디와 비밀번호를 정확히 입력했는데도 계속 오류가 발생합니다. 어떻게 해결할 수 있나요?")
                .createdAt(now.minusDays(3))
                .build();
        inquiryRepository.save(serviceInquiry);

        /* 2. 두 번째 회원에게 REPORT(신고/제보) 문의 – 신고 대상은 memberA */
        Inquiry reportInquiry = Inquiry.builder()
                .memberId(memberB.getMemberId())
                .targetMemberId(memberA.getMemberId())
                .type(InquiryType.REPORT)
                .title("거래 사기 의심 신고")
                .content("회원 A가 물품을 받지 못했는데도 반송 처리를 하지 않고 연락이 되지 않습니다. 사기 피해가 의심됩니다.")
                .createdAt(now.minusDays(2).minusHours(4))
                .build();
        inquiryRepository.save(reportInquiry);

        /* 3. 첫 번째 회원에게 DAMAGE(파손 신고) 문의 – 파손 대상은 memberB, 이미지 키 예시 포함 */
        Inquiry damageInquiry = Inquiry.builder()
                .memberId(memberA.getMemberId())
                .targetMemberId(memberB.getMemberId())
                .type(InquiryType.DAMAGE)
                .title("빌린 노트북 화면 파손 신고")
                .content("대여 중에 화면에 금이 가 생겼습니다. 첨부된 사진을 확인해 주시기 바랍니다.")
                .damageImageKeys(List.of(
                        "damage-images/memberA/2025-06-01_01.png",
                        "damage-images/memberA/2025-06-01_02.png"
                ))
                .processed(false)
                .createdAt(now.minusDays(1))
                .build();
        inquiryRepository.save(damageInquiry);

        System.out.println("[InquiryDummy] SERVICE, REPORT, DAMAGE 문의 각각 1건씩 생성완료");
    }
}
