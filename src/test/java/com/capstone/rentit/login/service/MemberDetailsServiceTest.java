package com.capstone.rentit.login.service;

import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.exception.MemberNotFoundException;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.member.status.GenderEnum;
import com.capstone.rentit.member.status.MemberRoleEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberDetailsServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberDetailsService memberDetailsService;

    @Test
    @DisplayName("loadUserByUsername - 존재하는 이메일로 사용자 로드 성공")
    void loadUserByUsername_success() {
        // given
        String email = "user@test.com";
        Student student = Student.builder()
                .memberId(10L)
                .email(email)
                .password("securePwd")
                .name("TestUser")
                .role(MemberRoleEnum.STUDENT)
                .nickname("nick")
                .phone("010-1234-5678")
                .university("Uni")
                .studentId("S001")
                .gender(GenderEnum.MEN)
                .build();
        when(memberRepository.findByEmail(email))
                .thenReturn(Optional.of(student));

        // when
        UserDetails userDetails = memberDetailsService.loadUserByUsername(email);

        // then
        assertThat(userDetails).isInstanceOf(MemberDetails.class);
        assertThat(userDetails.getUsername()).isEqualTo(email);
        assertThat(userDetails.getPassword()).isEqualTo("securePwd");
        // 권한은 ROLE_STUDENT 형태로 부여됨
        assertThat(userDetails.getAuthorities())
                .extracting(auth -> auth.getAuthority())
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_STUDENT");
    }

    @Test
    @DisplayName("loadUserByUsername - 미등록 이메일로 예외 발생")
    void loadUserByUsername_notFound() {
        // given
        String unknownEmail = "nouser@test.com";
        when(memberRepository.findByEmail(unknownEmail))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberDetailsService.loadUserByUsername(unknownEmail))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage("존재하지 않는 사용자 이메일 입니다.");
    }
}