package com.capstone.rentit.dummy;

import com.capstone.rentit.inquiry.repository.InquiryRepository;
import com.capstone.rentit.item.repository.ItemRepository;
import com.capstone.rentit.locker.repository.DeviceRepository;
import com.capstone.rentit.locker.repository.LockerRepository;
import com.capstone.rentit.member.domain.Admin;
import com.capstone.rentit.member.domain.Company;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.domain.StudentCouncilMember;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.notification.repository.NotificationRepository;
import com.capstone.rentit.payment.repository.PaymentRepository;
import com.capstone.rentit.payment.repository.WalletRepository;
import com.capstone.rentit.rental.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Order(1)
public class UserDummyDataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    private final DeviceRepository deviceRepository;
    private final LockerRepository lockerRepository;
    private final ItemRepository itemRepository;
    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final NotificationRepository notificationRepository;
    private final InquiryRepository inquiryRepository;

    @Override
    public void run(ApplicationArguments args) {
        inquiryRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        walletRepository.deleteAllInBatch();
        lockerRepository.deleteAllInBatch();
        deviceRepository.deleteAllInBatch();
        rentalRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
//        if (memberRepository.count() > 0) {return;}

        final int STUDENT_COUNT = 5;
        final int COMPANY_COUNT = 2;
        final int COUNCIL_COUNT = 2;

        LocalDate today = LocalDate.now();

        // 1) 학생 생성 (실제 사용자처럼 이름·닉네임·대학교 설정)
        String[] studentNames     = {"김민수", "이서연", "박지훈", "최지우", "정우진"};
        String[] studentNicknames = {"minsu_k", "seoyeon_l", "jihun_p", "jiwoo_c", "woojin_j"};
        String[] universities      = {"아주대학교"};
        String[] profiles = {"4a36d988-5718-44b5-b5aa-3991fd425e21.png"
                , "5dfce88b-6564-4fa3-8365-b66881fdaa19.png"
                , "c3930f16-da0c-44ce-8b29-5611e3ea5a52.png"
                , "b0e6c6db-b1f9-45ab-9721-3bd4ed6f49f4.png"
                , "bd9d621c-ed59-45bc-ba80-3e253ef30fcb.png"};

        for (int i = 1; i <= STUDENT_COUNT; i++) {
            Student stu = Student.builder()
                    .email(String.format("student%02d@example.com", i))       // 이메일 그대로
                    .password(passwordEncoder.encode("password"))             // 패스워드 그대로
                    .name(studentNames[i - 1])
                    .nickname("Nick" + studentNicknames[i - 1])
                    .profileImg(profiles[i - 1])
                    .role(MemberRoleEnum.STUDENT)
                    .locked(false)
                    .createdAt(today)
                    .studentId("S" + (1000 + i))
                    .university(universities[(i - 1) % universities.length])
                    .nickname(studentNicknames[i - 1])
                    .phone(String.format("010-1234-%04d",
                            ThreadLocalRandom.current().nextInt(0, 10_000)))
                    .build();
            memberRepository.save(stu);
        }

        // 2) 기업 생성 (변경 없이)
        for (int i = 1; i <= COMPANY_COUNT; i++) {
            Company comp = Company.builder()
                    .email(String.format("company%02d@example.com", i))
                    .contactEmail(String.format("company%02d@example.com", i))
                    .password(passwordEncoder.encode("password"))
                    .name("기업 " + i)
                    .nickname("기업 " + i)
                    .role(MemberRoleEnum.COMPANY)
                    .locked(false)
                    .createdAt(today)
                    .companyName("기업" + i)
                    .description("기업 설명" + i)
                    .industry("기업 분야" + i)
                    .registrationNumber("사업자 번호" + i)
                    .build();
            memberRepository.save(comp);
        }

        // 3) 학생회 회원 생성 (변경 없이)
        for (int i = 1; i <= COUNCIL_COUNT; i++) {
            StudentCouncilMember council = StudentCouncilMember.builder()
                    .email(String.format("council%02d@example.com", i))
                    .contactEmail(String.format("council%02d@example.com", i))
                    .password(passwordEncoder.encode("password"))
                    .name("학생회 " + i)
                    .nickname("학생회 " + i)
                    .role(MemberRoleEnum.COUNCIL)
                    .locked(false)
                    .createdAt(today)
                    .university(universities[(i - 1) % universities.length])
                    .description(universities[(i - 1) % universities.length] + " 학생회")
                    .build();
            memberRepository.save(council);
        }

        // 4) 관리자 생성
        Admin admin = Admin.builder()
                .email("admin01@example.com")
                .password(passwordEncoder.encode("password"))
                .name("관리자1")
                .nickname("관리자1")
                .role(MemberRoleEnum.ADMIN)
                .locked(false)
                .createdAt(today)
                .build();
        memberRepository.save(admin);

        System.out.printf("[UserDummyDataInitializer] Generated %d students, %d companies, %d councils.%n",
                STUDENT_COUNT, COMPANY_COUNT, COUNCIL_COUNT);
    }
}
