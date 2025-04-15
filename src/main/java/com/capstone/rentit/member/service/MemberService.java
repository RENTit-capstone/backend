package com.capstone.rentit.member.service;

import com.capstone.rentit.common.MemberRoleEnum;
import com.capstone.rentit.login.dto.JwtToken;
import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.member.domain.Company;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.domain.StudentCouncilMember;
import com.capstone.rentit.member.dto.*;
import com.capstone.rentit.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
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
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public JwtToken signIn(String username, String password) {
        // 1. username + password 를 기반으로 Authentication 객체 생성
        // 이때 authentication 은 인증 여부를 확인하는 authenticated 값이 false
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);

        // 2. 실제 검증. authenticate() 메서드를 통해 요청된 Member 에 대한 검증 진행
        // authenticate 메서드가 실행될 때 CustomUserDetailsService 에서 만든 loadUserByUsername 메서드 실행
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        return jwtTokenProvider.generateToken(authentication);
    }

    public Long createMember(MemberCreateForm form) {
        Member member;
        if (form instanceof StudentCreateForm stuForm) {
            member = Student.builder()
                    .name(stuForm.getName())
                    .role(MemberRoleEnum.STUDENT)
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
                    .role(MemberRoleEnum.COUNCIL)
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
                    .role(MemberRoleEnum.COMPANY)
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
        return memberRepository.save(member).getMemberId();
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