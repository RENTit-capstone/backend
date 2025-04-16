package com.capstone.rentit.member.service;

import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.MemberCreateForm;
import com.capstone.rentit.member.dto.StudentCreateForm;
import com.capstone.rentit.member.dto.StudentUpdateForm;
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
        // 비밀번호는 암호화 되었으므로 평문과는 다르지만, 매칭 여부를 확인
        assertTrue(passwordEncoder.matches("password", student.getPassword()));
        assertEquals("Integration University", student.getUniversity());
    }

    // --------------------------
    // 조회 (getUser, findByEmail) 테스트
    // --------------------------
    @Test
    void getUser_and_findByEmail_success() {
        // 회원 생성
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

        // getUser 테스트
        Optional<Member> memberOpt = memberService.getUser(id);
        assertTrue(memberOpt.isPresent());
        assertEquals("bob@test.com", memberOpt.get().getEmail());

        // findByEmail 테스트
        Optional<Member> emailOpt = memberService.findByEmail("bob@test.com");
        assertTrue(emailOpt.isPresent());
        assertEquals("Bob", emailOpt.get().getName());
    }

    // --------------------------
    // 업데이트 (updateUser) 테스트 - 학생 회원 업데이트
    // --------------------------
    @Test
    void updateUser_student_success() {
        // 초기 학생 회원 생성
        StudentCreateForm form = new StudentCreateForm();
        form.setName("Alice");
        form.setEmail("alice@test.com");
        form.setPassword("password");
        form.setNickname("aliceOld");
        form.setPhone("010-2222-2222");
        form.setUniversity("Old University");
        form.setStudentId("S2020");
        form.setGender("F");
        Long memberId = memberService.createMember(form);

        // 업데이트 요청 - StudentUpdateForm을 이용해서 이름, 프로필 이미지, 닉네임, 전화번호를 변경
        StudentUpdateForm updateForm = new StudentUpdateForm();
        updateForm.setName("Alice Updated");
        updateForm.setProfileImg("newProfile.jpg");
        updateForm.setNickname("aliceNew");
        updateForm.setPhone("010-3333-3333");

        Member updatedMember = memberService.updateUser(memberId, updateForm);
        Student updatedStudent = (Student) updatedMember;
        assertEquals("Alice Updated", updatedStudent.getName());
        assertEquals("newProfile.jpg", updatedStudent.getProfileImg());
        assertEquals("aliceNew", updatedStudent.getNickname());
        assertEquals("010-3333-3333", updatedStudent.getPhone());
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
        Long memberId = memberService.createMember(form);

        // 생성된 회원이 존재하는지 확인
        Optional<Member> beforeDelete = memberService.getUser(memberId);
        assertTrue(beforeDelete.isPresent());

        // 삭제 실행
        memberService.deleteUser(memberId);

        Optional<Member> afterDelete = memberService.getUser(memberId);
        assertFalse(afterDelete.isPresent());
    }
}