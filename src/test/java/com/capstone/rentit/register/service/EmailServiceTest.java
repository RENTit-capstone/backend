package com.capstone.rentit.register.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // JUnit5에서 Mockito를 사용하기 위한 확장
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender mailSender;

    @Test
    @DisplayName("성공: 주어진 이메일과 코드로 인증 메일을 올바르게 생성하고 발송을 시도해야 한다")
    void sendVerificationEmail_shouldConstructAndSendCorrectEmail() {
        // Given (주어진 환경)
        String to = "test@gmail.com";
        String otp = "123456";
        String expectedSubject = "RENTit 회원가입 이메일 인증 코드";
        String expectedText = "인증 코드는 " + otp + " 입니다.\n5분 이내에 입력해주세요.";

        // ArgumentCaptor: Mock 객체(mailSender)의 메소드가 호출될 때 전달된 인자를 캡처합니다.
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // When (테스트할 동작 실행)
        emailService.sendVerificationEmail(to, otp);

        // Then (결과 검증)
        // 1. mailSender의 send() 메소드가 SimpleMailMessage 객체를 인자로 받아 1번 호출되었는지 검증합니다.
        verify(mailSender, times(1)).send(messageCaptor.capture());

        // 2. 캡처된 SimpleMailMessage 객체를 가져옵니다.
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        // 3. 캡처된 객체의 필드가 우리가 기대한 값과 일치하는지 상세하게 검증합니다. (AssertJ 사용)
        assertThat(sentMessage.getTo()).contains(to);
        assertThat(sentMessage.getSubject()).isEqualTo(expectedSubject);
        assertThat(sentMessage.getText()).isEqualTo(expectedText);
    }
}