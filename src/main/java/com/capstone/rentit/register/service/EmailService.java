package com.capstone.rentit.register.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("RENTit 회원가입 이메일 인증 코드");
        message.setText("인증 코드는 " + otp + " 입니다.\n5분 이내에 입력해주세요.");
        mailSender.send(message);
    }
}