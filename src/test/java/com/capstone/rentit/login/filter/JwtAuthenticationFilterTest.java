package com.capstone.rentit.login.filter;

import com.capstone.rentit.login.provider.JwtTokenProvider;
import com.capstone.rentit.login.service.MemberDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private MemberDetailsService memberDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 JWT가 헤더에 있으면 SecurityContext에 Authentication이 설정된다")
    void doFilterInternal_validToken_setsAuthentication() throws ServletException, IOException {
        // given
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUsername(token)).thenReturn("user@test.com");
        UserDetails userDetails = new User(
                "user@test.com",
                "pwd",
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(memberDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getName()).isEqualTo("user@test.com");
        assertThat(auth.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_USER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT 헤더가 없거나 유효하지 않으면 Authentication이 설정되지 않고 필터 체인은 호출된다")
    void doFilterInternal_noOrInvalidToken_doesNotSetAuthentication() throws ServletException, IOException {
        // case1: 헤더 없음
        filter.doFilterInternal(request, response, filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);

        // case2: 유효하지 않은 토큰
        SecurityContextHolder.clearContext();
        request.addHeader("Authorization", "Bearer bad.token");
        when(tokenProvider.validateToken("bad.token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(2)).doFilter(request, response);
    }
}