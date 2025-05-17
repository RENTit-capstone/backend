package com.capstone.rentit.notification.service;

import com.capstone.rentit.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock MemberRepository memberRepository;

    private DeviceTokenService service() {
        return new DeviceTokenService(memberRepository);
    }

    @Test
    @DisplayName("token이 유효하면 updateFcmToken()을 호출한다")
    void saveToken_success() {
        // given
        Long memberId = 1L;
        String token   = "valid-token";

        DeviceTokenService sut = service();

        // when
        sut.saveToken(memberId, token);

        // then
        verify(memberRepository, times(1)).updateFcmToken(memberId, token);
    }

    @Test
    @DisplayName("token이 null이면 IllegalArgumentException을 던지고, DB 호출이 없다")
    void saveToken_nullToken_throws() {
        DeviceTokenService sut = service();

        assertThatThrownBy(() -> sut.saveToken(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("token");

        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("token이 공백이면 IllegalArgumentException을 던지고, DB 호출이 없다")
    void saveToken_blankToken_throws() {
        DeviceTokenService sut = service();

        assertThatThrownBy(() -> sut.saveToken(1L, "   "))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(memberRepository);
    }
}
