package com.capstone.rentit.login.dto;

import com.capstone.rentit.member.status.MemberRoleEnum;
import com.capstone.rentit.member.domain.Member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class MemberDetails implements UserDetails {

    private final Member member;

    private boolean isUser(MemberRoleEnum role){
        return role != MemberRoleEnum.ADMIN;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(!isUser(member.getRole())){
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        else{
            List<String> roles = List.of("ROLE_USER", "ROLE_" + MemberRoleEnum.roleToString(member.getRole()));
            return roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
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
