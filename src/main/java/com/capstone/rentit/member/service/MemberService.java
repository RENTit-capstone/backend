package com.capstone.rentit.member.service;

import com.capstone.rentit.common.MemberRoleConverter;
import com.capstone.rentit.member.domain.Company;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.domain.StudentCouncilMember;
import com.capstone.rentit.member.dto.CompanyUpdateForm;
import com.capstone.rentit.member.dto.MemberUpdateForm;
import com.capstone.rentit.member.dto.StudentCouncilMemberUpdateForm;
import com.capstone.rentit.member.dto.StudentUpdateForm;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.register.dto.StudentRegisterForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public Long createStudent(StudentRegisterForm form) {
        Member member = Student.builder()
                .name(form.getName())
                .role(MemberRoleConverter.fromInteger(form.getRole()))
                .email(form.getEmail())
                .password(passwordEncoder.encode(form.getPassword()))
                .locked(false)
                .gender(form.getGender())
                .studentId(form.getStudentId())
                .university(form.getUniversity())
                .nickname(form.getNickname())
                .createdAt(LocalDate.now())
                .phone(form.getPhone())
                .build();
        return memberRepository.save(member).getId();
    }

    public Optional<Member> getUser(Long id) {
        return memberRepository.findById(id);
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public List<Member> getAllUsers() {
        return memberRepository.findAll();
    }

    public Member updateUser(Long id, MemberUpdateForm updateForm) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 역할에 따른 업데이트
        if (member instanceof Student student && updateForm instanceof StudentUpdateForm stuForm) {
            student.updateStudent(
                    stuForm.getName(),
                    stuForm.getProfileImg(),
                    stuForm.getNickname(),
                    stuForm.getPhone()
            );
        }
        else if (member instanceof StudentCouncilMember scm && updateForm instanceof StudentCouncilMemberUpdateForm scmForm) {
            scm.updateCouncilMember(
                    scmForm.getName(),
                    scmForm.getProfileImg()
            );
        }
        else if (member instanceof Company company && updateForm instanceof CompanyUpdateForm companyForm) {
            company.updateCompany(
                    companyForm.getName(),
                    companyForm.getProfileImg(),
                    companyForm.getCompanyName()
            );
        }
        return memberRepository.save(member);
    }

    public void deleteUser(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        memberRepository.delete(member);
    }
}