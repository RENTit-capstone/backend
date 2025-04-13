package com.capstone.rentit.member.service;

import com.capstone.rentit.common.MemberRoleConverter;
import com.capstone.rentit.member.domain.Company;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.domain.StudentCouncilMember;
import com.capstone.rentit.member.dto.*;
import com.capstone.rentit.member.repository.MemberRepository;
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

    public Long createMember(MemberCreateForm form) {
        Member member;
        if (form instanceof StudentCreateForm stuForm) {
            member = Student.builder()
                    .name(stuForm.getName())
                    .role(MemberRoleConverter.fromInteger(stuForm.getRole()))
                    .email(stuForm.getEmail())
                    .password(passwordEncoder.encode(stuForm.getPassword()))
                    .locked(false)
                    .createdAt(LocalDate.now())
                    .gender(stuForm.getGender())
                    .studentId(stuForm.getStudentId())
                    .university(stuForm.getUniversity())
                    .nickname(stuForm.getNickname())
                    .phone(stuForm.getPhone())
                    .build();
        }
        else if (form instanceof StudentCouncilMemberCreateForm scmForm) {
            member = Student.builder()
                    .name(scmForm.getName())
                    .role(MemberRoleConverter.fromInteger(scmForm.getRole()))
                    .email(scmForm.getEmail())
                    .password(passwordEncoder.encode(scmForm.getPassword()))
                    .locked(false)
                    .createdAt(LocalDate.now())
                    .gender(scmForm.getUniversity())
                    .build();
        }
        else if (form instanceof CompanyCreateForm comForm) {
            member = Student.builder()
                    .name(comForm.getName())
                    .role(MemberRoleConverter.fromInteger(comForm.getRole()))
                    .email(comForm.getEmail())
                    .password(passwordEncoder.encode(comForm.getPassword()))
                    .locked(false)
                    .createdAt(LocalDate.now())
                    .gender(comForm.getCompanyName())
                    .build();
        }
        else {
            throw new IllegalArgumentException("CreateMember: Unsupported member type");
        }
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
        else {
            throw new IllegalArgumentException("UpdateUser: Unsupported member type");
        }
        return memberRepository.save(member);
    }

    public void deleteUser(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        memberRepository.delete(member);
    }
}