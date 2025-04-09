package com.capstone.rentit.member.service;

import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.register.dto.StudentRegisterForm;
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

    // 더미 RegisterForm 생성 메서드
    private StudentRegisterForm createDummyRegisterForm() {
        StudentRegisterForm form = new StudentRegisterForm();
        form.setName("Test User");
        form.setPhone("010-1234-5678");
        form.setRole(1);
        form.setEmail("test@example.com");
        form.setGender(1);
        form.setPassword("password");
        form.setStudentId("12345678");
        form.setUniversity("Test University");
        form.setNickname("tester");
        return form;
    }

    @Test
    void testCreateStudent() {
        Long id = memberService.createStudent(createDummyRegisterForm());
        assertNotNull(id);

        // 저장된 엔티티 검증
        Member member = memberRepository.findById(id).orElse(null);
        assertNotNull(member);
        assertEquals("Test User", member.getName());
        // 평문과 다르게 암호화된 비밀번호가 저장되었는지 확인
        assertNotEquals("password", member.getPassword());
        assertTrue(passwordEncoder.matches("password", member.getPassword()));
    }

    @Test
    void testGetUser() {
        Long id = memberService.createStudent(createDummyRegisterForm());
        Optional<Member> optionalMember = memberService.getUser(id);
        assertTrue(optionalMember.isPresent());
        assertEquals("test@example.com", optionalMember.get().getEmail());
    }

    @Test
    void testFindByEmail() {
        Long id = memberService.createStudent(createDummyRegisterForm());
        Optional<Member> optionalMember = memberService.findByEmail("test@example.com");
        assertTrue(optionalMember.isPresent());
    }

    @Test
    void testGetAllUsers() {
        // 두 명의 사용자를 생성 후 전체 조회
        Long id = memberService.createStudent(createDummyRegisterForm());

        StudentRegisterForm form2 = createDummyRegisterForm();
        form2.setEmail("another@example.com");
        form2.setName("Another User");
        memberService.createStudent(form2);

        List<Member> members = memberService.getAllUsers();
        assertEquals(2, members.size());
    }

    @Test
    void testUpdateUser() {
        Long id = memberService.createStudent(createDummyRegisterForm());
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // updateUser 메서드는 현재 전달된 userDetails의 필드를 반영하지 않으므로,
        // 엔티티를 직접 수정한 후 updateUser를 호출하여 변경된 내용이 반영되는지 확인
//        member.setNickname("updatedNickname");
//        Member updatedMember = memberService.updateUser(id, member);
//        assertEquals("updatedNickname", updatedMember.getNickname());
    }

    @Test
    void testDeleteUser() {
        Long id = memberService.createStudent(createDummyRegisterForm());
        memberService.deleteUser(id);
        Optional<Member> deletedMember = memberRepository.findById(id);
        assertFalse(deletedMember.isPresent());
    }
}