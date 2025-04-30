package com.capstone.rentit.login.service;

import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.exception.MemberNotFoundException;
import com.capstone.rentit.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = findMemberByEmail(username);
        return new MemberDetails(member);
    }

    private Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() ->
                        new MemberNotFoundException("존재하지 않는 사용자 이메일 입니다."));
    }
}
