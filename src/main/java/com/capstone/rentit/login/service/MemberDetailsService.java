package com.capstone.rentit.login.service;

import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.member.domain.Member;
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
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found with email: " + username));
        return new MemberDetails(member);
    }
}
