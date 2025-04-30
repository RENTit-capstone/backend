package com.capstone.rentit.member.service;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.member.dto.*;
import com.capstone.rentit.member.domain.*;
import com.capstone.rentit.member.exception.*;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.register.exception.EmailAlreadyRegisteredException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public Long createMember(MemberCreateForm form) {
        Member member = Member.createEntity(form, passwordEncoder.encode(form.getPassword()));
        return memberRepository.save(member).getMemberId();
    }

    @Transactional(readOnly = true)
    public MemberDto getMemberById(Long id) {
        Member member = findMemberById(id);
        return MemberDto.fromEntity(member);
    }

    @Transactional(readOnly = true)
    public MemberDto getMemberByEmail(String email) {
        Member member = findMemberByEmail(email);
        return MemberDto.fromEntity(member);
    }

    @Transactional(readOnly = true)
    public List<MemberDto> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(MemberDto::fromEntity)
                .collect(Collectors.toList());
    }

    public void updateMember(Long id, MemberUpdateForm form) {
        Member member = findMemberById(id);

        member.update(form);
    }
    public void updateProfileImage(Long id, MultipartFile file) {
        Member member = findMemberById(id);
        String objectKey = fileStorageService.store(file);

        member.updateProfile(objectKey);
    }

    /** 6) 회원 삭제 */
    public void deleteMember(Long id) {
        Member member = findMemberById(id);
        memberRepository.delete(member);
    }

    public void ensureEmailNotRegistered(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    private Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() ->
                        new MemberNotFoundException("존재하지 않는 사용자 ID 입니다."));
    }

    private Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() ->
                        new MemberNotFoundException("존재하지 않는 사용자 이메일 입니다."));
    }
}
