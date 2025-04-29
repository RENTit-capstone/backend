package com.capstone.rentit.member.service;

import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.member.domain.Company;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.domain.StudentCouncilMember;
import com.capstone.rentit.member.dto.*;
import com.capstone.rentit.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceUnitTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Captor
    private ArgumentCaptor<Student> studentCaptor;

    @Captor
    private ArgumentCaptor<Company> companyCaptor;

    @Captor
    private ArgumentCaptor<StudentCouncilMember> councilCaptor;

    @Test
    @DisplayName("학생 회원 생성: ID를 반환해야 한다")
    void createMember_shouldCreateStudent() {
        // given
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(memberRepository.save(any(Student.class)))
                .thenAnswer(invocation -> {
                    Student s = invocation.getArgument(0);
                    return Student.builder()
                            .memberId(1L)
                            .name(s.getName())
                            .email(s.getEmail())
                            .password(s.getPassword())
                            .nickname(s.getNickname())
                            .phone(s.getPhone())
                            .university(s.getUniversity())
                            .studentId(s.getStudentId())
                            .gender(s.getGender())
                            .role(s.getRole())
                            .build();
                });
        StudentCreateForm form = new StudentCreateForm();
        form.setName("Stu"); form.setEmail("stu@test.com"); form.setPassword("pass");
        form.setNickname("nick"); form.setPhone("010-0000-0000");
        form.setUniversity("Uni"); form.setStudentId("S100"); form.setGender("F");

        // when
        Long id = memberService.createMember(form);

        // then
        assertThat(id).isEqualTo(1L);
        verify(memberRepository).save(studentCaptor.capture());
        Student captured = studentCaptor.getValue();
        assertThat(captured.getRole()).isEqualTo(MemberRoleEnum.STUDENT);
        assertThat(captured.getPassword()).isEqualTo("encoded");
    }

    @Test
    @DisplayName("회사 회원 생성: ID를 반환해야 한다")
    void createMember_shouldCreateCompany() {
        // given
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(memberRepository.save(any(Company.class)))
                .thenAnswer(invocation -> {
                    Company c = invocation.getArgument(0);
                    return Company.builder()
                            .memberId(2L)
                            .name(c.getName())
                            .email(c.getEmail())
                            .password(c.getPassword())
                            .companyName(c.getCompanyName())
                            .role(c.getRole())
                            .build();
                });
        CompanyCreateForm form = new CompanyCreateForm();
        form.setName("Comp"); form.setEmail("comp@test.com");
        form.setPassword("pwd"); form.setCompanyName("Company Inc.");

        // when
        Long id = memberService.createMember(form);

        // then
        assertThat(id).isEqualTo(2L);
        verify(memberRepository).save(companyCaptor.capture());
        Company captured = companyCaptor.getValue();
        assertThat(captured.getRole()).isEqualTo(MemberRoleEnum.COMPANY);
        assertThat(captured.getPassword()).isEqualTo("encoded");
    }

    @Test
    @DisplayName("학생회 회원 생성: ID를 반환해야 한다")
    void createMember_shouldCreateCouncilMember() {
        // given
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(memberRepository.save(any()))
                .thenAnswer(invocation -> {
                    StudentCouncilMember m = invocation.getArgument(0);
                    return StudentCouncilMember.builder()
                            .memberId(3L)
                            .name(m.getName())
                            .email(m.getEmail())
                            .password(m.getPassword())
                            .university(m.getUniversity())
                            .role(m.getRole())
                            .build();
                });
        StudentCouncilMemberCreateForm form = new StudentCouncilMemberCreateForm();
        form.setName("Council"); form.setEmail("council@test.com");
        form.setPassword("pwd"); form.setUniversity("UniCouncil");

        // when
        Long id = memberService.createMember(form);

        // then
        assertThat(id).isEqualTo(3L);
        verify(memberRepository).save(councilCaptor.capture());
        StudentCouncilMember captured = councilCaptor.getValue();
        assertThat(captured.getRole()).isEqualTo(MemberRoleEnum.COUNCIL);
        assertThat(captured.getPassword()).isEqualTo("encoded");
    }

    @Test
    @DisplayName("지원되지 않은 폼: 예외를 던져야 한다")
    void createMember_shouldThrowUnsupportedForm() {
        // given
        MemberCreateForm bad = new MemberCreateForm() {};

        // when & then
        assertThatThrownBy(() -> memberService.createMember(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CreateMember: Unsupported member type");
    }

    @Test
    @DisplayName("사용자 조회: ID 및 이메일로 조회되어야 한다")
    void retrieveUsers_shouldReturnUsers() {
        // given
        Student student = Student.builder()
                .memberId(4L)
                .email("bob@test.com")
                .name("Bob")
                .role(MemberRoleEnum.STUDENT)
                .build();
        when(memberRepository.findById(4L)).thenReturn(Optional.of(student));
        when(memberRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(student));
        when(memberRepository.findAll()).thenReturn(Collections.singletonList(student));

        // when
        Optional<Member> byId = memberService.getMember(4L);
        Optional<Member> byEmail = memberService.findByEmail("bob@test.com");
        List<Member> all = memberService.getAllMembers();

        // then
        assertThat(byId).isPresent().contains(student);
        assertThat(byEmail).isPresent().contains(student);
        assertThat(all).hasSize(1).contains(student);
    }

    @Test
    @DisplayName("회원 정보 업데이트: 학생 필드가 업데이트되어야 한다")
    void updateUser_shouldUpdateStudent() {
        // given
        Student existing = Student.builder().memberId(5L).role(MemberRoleEnum.STUDENT).build();
        when(memberRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(memberRepository.save(any(Student.class))).thenReturn(existing);

        StudentUpdateForm form = new StudentUpdateForm();
        form.setName("NewName"); form.setProfileImg("img.png");
        form.setNickname("nick2"); form.setPhone("010-1111-2222");

        // when
        Member updated = memberService.updateMember(5L, form);

        // then
        assertThat(((Student) updated).getName()).isEqualTo("NewName");
        assertThat(((Student) updated).getProfileImg()).isEqualTo("img.png");
    }

    @Test
    @DisplayName("회원 정보 업데이트 실패: 예외 메시지를 확인한다")
    void updateMember_shouldThrowOnTypeMismatch() {
        // given
        Company company = Company.builder().memberId(6L).role(MemberRoleEnum.COMPANY).build();
        when(memberRepository.findById(6L)).thenReturn(Optional.of(company));
        StudentUpdateForm form = new StudentUpdateForm();

        // when & then
        assertThatThrownBy(() -> memberService.updateMember(6L, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UpdateUser: Unsupported member type");
    }

    @Test
    @DisplayName("회원 삭제: 기존 사용자를 삭제해야 한다")
    void deleteUser_shouldDeleteMember() {
        // given
        Student dummy = Student.builder().memberId(7L).role(MemberRoleEnum.STUDENT).build();
        when(memberRepository.findById(7L)).thenReturn(Optional.of(dummy));

        // when
        memberService.deleteMember(7L);

        // then
        verify(memberRepository).delete(dummy);
    }

    @Test
    @DisplayName("회원 삭제 실패: 예외 메시지를 확인한다")
    void deleteMember_shouldThrowOnNotFound() {
        // given
        when(memberRepository.findById(8L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.deleteMember(8L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}
