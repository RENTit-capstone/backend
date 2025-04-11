package com.capstone.rentit.login.dto;

import com.capstone.rentit.common.MemberRoleConverter;
import com.capstone.rentit.member.domain.Member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@RequiredArgsConstructor
public class MemberDetails implements UserDetails {

    private final Member member;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + MemberRoleConverter.fromInteger(member.getRole().ordinal())));
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 추가 비즈니스 로직이 있다면 이곳에서 처리
    }

    @Override
    public boolean isAccountNonLocked() {
        return !member.isLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 추가 비즈니스 로직이 있다면 이곳에서 처리
    }

    @Override
    public boolean isEnabled() {
        return !member.isLocked();
    }
}
