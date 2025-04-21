package com.capstone.rentit.member.service;

import com.capstone.rentit.common.MemberRoleEnum;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.*;
import com.capstone.rentit.member.repository.MemberRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --------------------------
    // 생성 (createMember) 테스트 - 학생 회원 생성
    // --------------------------
    @Test
    void createMember_student_success() {
        StudentCreateForm form = new StudentCreateForm();
        form.setName("Integration Student");
        form.setEmail("integration@student.com");
        form.setPassword("password");
        form.setNickname("intStudent");
        form.setPhone("010-0000-0000");
        form.setUniversity("Integration University");
        form.setStudentId("INT1000");
        form.setGender("F");

        Long memberId = memberService.createMember(form);
        assertNotNull(memberId);

        Optional<Member> optMember = memberRepository.findById(memberId);
        assertTrue(optMember.isPresent());
        Student student = (Student) optMember.get();

        assertEquals("Integration Student", student.getName());
        assertEquals("integration@student.com", student.getEmail());
        assertTrue(passwordEncoder.matches("password", student.getPassword()));
        assertEquals("Integration University", student.getUniversity());
    }

    // --------------------------
    // 생성 - 학생회 회원 생성
    // --------------------------
    @Test
    void createMember_council_success() {
        StudentCouncilMemberCreateForm form = new StudentCouncilMemberCreateForm();
        form.setName("Council User");
        form.setEmail("council@test.com");
        form.setPassword("pwd");
        form.setUniversity("Council University");

        Long id = memberService.createMember(form);
        Optional<Member> opt = memberRepository.findById(id);
        assertTrue(opt.isPresent());
        Member m = opt.get();
        assertEquals(MemberRoleEnum.COUNCIL, m.getRole());
        assertEquals("council@test.com", m.getEmail());
    }

    // --------------------------
    // 생성 - 회사 회원 생성
    // --------------------------
    @Test
    void createMember_company_success() {
        CompanyCreateForm form = new CompanyCreateForm();
        form.setName("Company Inc.");
        form.setEmail("company@test.com");
        form.setPassword("compwd");
        form.setCompanyName("Company Inc.");

        Long id = memberService.createMember(form);
        Optional<Member> opt = memberRepository.findById(id);
        assertTrue(opt.isPresent());
        Member m = opt.get();
        assertEquals(MemberRoleEnum.COMPANY, m.getRole());
        assertEquals("company@test.com", m.getEmail());
    }

    // --------------------------
    // 생성 - 지원되지 않는 폼
    // --------------------------
    @Test
    void createMember_unsupportedForm_throws() {
        MemberCreateForm badForm = new MemberCreateForm() {};
        assertThrows(IllegalArgumentException.class, () -> {
            memberService.createMember(badForm);
        });
    }

    // --------------------------
    // 조회 (getUser, findByEmail, getAllUsers) 테스트
    // --------------------------
    @Test
    void getUser_and_findByEmail_and_getAllUsers_success() {
        StudentCreateForm form = new StudentCreateForm();
        form.setName("Bob");
        form.setEmail("bob@test.com");
        form.setPassword("password");
        form.setNickname("bobNick");
        form.setPhone("010-1111-1111");
        form.setUniversity("Test University");
        form.setStudentId("S1010");
        form.setGender("M");
        Long id = memberService.createMember(form);

        Optional<Member> byId = memberService.getUser(id);
        assertTrue(byId.isPresent());
        assertEquals("bob@test.com", byId.get().getEmail());

        Optional<Member> byEmail = memberService.findByEmail("bob@test.com");
        assertTrue(byEmail.isPresent());
        assertEquals("Bob", byEmail.get().getName());

        List<Member> all = memberService.getAllUsers();
        assertTrue(all.size() >= 1);
    }

    // --------------------------
    // 업데이트 (updateUser) 테스트 - 학생 회원 업데이트
    // --------------------------
    @Test
    void updateUser_student_success() {
        StudentCreateForm form = new StudentCreateForm();
        form.setName("Alice");
        form.setEmail("alice@test.com");
        form.setPassword("password");
        form.setNickname("aliceOld");
        form.setPhone("010-2222-2222");
        form.setUniversity("Old University");
        form.setStudentId("S2020");
        form.setGender("F");
        Long id = memberService.createMember(form);

        StudentUpdateForm update = new StudentUpdateForm();
        update.setName("Alice New");
        update.setProfileImg("img.jpg");
        update.setNickname("newNick");
        update.setPhone("010-3333-3333");

        Member updated = memberService.updateUser(id, update);
        Student st = (Student) updated;
        assertEquals("Alice New", st.getName());
        assertEquals("img.jpg", st.getProfileImg());
    }

    // --------------------------
    // 업데이트 - 타입 불일치 예외
    // --------------------------
    @Test
    void updateUser_unsupported_throws() {
        StudentCreateForm form = new StudentCreateForm();
        form.setName("Tom");
        form.setEmail("tom@test.com");
        form.setPassword("pwd");
        form.setNickname("tomNick");
        form.setPhone("010-5555-5555");
        form.setUniversity("Uni");
        form.setStudentId("S5050");
        form.setGender("M");
        Long id = memberService.createMember(form);

        CompanyUpdateForm wrongForm = new CompanyUpdateForm();
        wrongForm.setName("X");
        wrongForm.setProfileImg("x.jpg");
        wrongForm.setCompanyName("XCo");

        assertThrows(IllegalArgumentException.class, () -> {
            memberService.updateUser(id, wrongForm);
        });
    }

    // --------------------------
    // 삭제 (deleteUser) 테스트
    // --------------------------
    @Test
    void deleteUser_success() {
        StudentCreateForm form = new StudentCreateForm();
        form.setName("Charlie");
        form.setEmail("charlie@test.com");
        form.setPassword("password");
        form.setNickname("charlieNick");
        form.setPhone("010-4444-4444");
        form.setUniversity("Test University");
        form.setStudentId("S3030");
        form.setGender("M");
        Long id = memberService.createMember(form);

        assertTrue(memberService.getUser(id).isPresent());
        memberService.deleteUser(id);
        assertFalse(memberService.getUser(id).isPresent());
    }

    // --------------------------
    // 삭제 - 존재하지 않는 사용자 예외
    // --------------------------
    @Test
    void deleteUser_notFound_throws() {
        assertThrows(RuntimeException.class, () -> memberService.deleteUser(9999L));
    }
}