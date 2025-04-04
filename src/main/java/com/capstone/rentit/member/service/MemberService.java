package com.capstone.rentit.member.service;

import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.register.dto.RegisterForm;
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

    public Long createUser(RegisterForm form) {
        Member member = Member.builder()
                .name(form.getName())
                .phone(form.getPhone())
                .role(form.getRole())
                .email(form.getEmail())
                .gender(form.getGender())
                .password(passwordEncoder.encode(form.getPassword()))
                .studentId(form.getStudentId())
                .university(form.getUniversity())
                .nickname(form.getNickname())
                .createdAt(LocalDate.now())
                .isLocked(false)
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

    public Member updateUser(Long id, Member userDetails) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return memberRepository.save(member);
    }

    public void deleteUser(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        memberRepository.delete(member);
    }
}