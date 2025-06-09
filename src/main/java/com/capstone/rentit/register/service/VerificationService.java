package com.capstone.rentit.register.service;

import com.capstone.rentit.register.exception.InvalidVerificationCodeException;
import com.capstone.rentit.register.exception.UnivNotCertifiedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final StringRedisTemplate redis;
    private final EmailService emailService;

    private static final int CODE_LENGTH = 6;
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10);

    private static final String CODE_KEY_PREFIX = "verification-code:";
    private static final String VERIFIED_KEY_PREFIX = "verified-email:";


    public void generateAndSendVerificationCode(String email) {
        String code = buildNumericCode(CODE_LENGTH);
        redis.opsForValue().set(codeKey(email), code, CODE_TTL);
        emailService.sendVerificationEmail(email, code);
    }

    public void verifyCode(String email, int userCode) {
        String storedCode = redis.opsForValue().get(codeKey(email));

        if (storedCode == null || !storedCode.equals(String.valueOf(userCode))) {
            throw new InvalidVerificationCodeException();
        }

        redis.delete(codeKey(email));
        markEmailAsVerified(email);
    }


    public void markEmailAsVerified(String email) {
        redis.opsForValue().set(verifiedKey(email), "true", VERIFIED_TTL);
    }

    public void ensureEmailVerified(String email) {
        String isVerified = redis.opsForValue().get(verifiedKey(email));
        if (!"true".equals(isVerified)) {
            throw new UnivNotCertifiedException();
        }
    }

    public void clearVerification(String email) {
        // '인증 완료' 상태 키 삭제
        redis.delete(verifiedKey(email));
    }

    private String buildNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(rnd.nextInt(0, 10));
        }
        return sb.toString();
    }

    public void clearAllVerifications() {
        // 'verification-code:' 로 시작하는 모든 키를 찾습니다.
        Set<String> codeKeys = redis.keys(CODE_KEY_PREFIX + "*");
        if (codeKeys != null && !codeKeys.isEmpty()) {
            redis.delete(codeKeys);
        }

        // 'verified-email:' 로 시작하는 모든 키를 찾습니다.
        Set<String> verifiedKeys = redis.keys(VERIFIED_KEY_PREFIX + "*");
        if (verifiedKeys != null && !verifiedKeys.isEmpty()) {
            redis.delete(verifiedKeys);
        }
    }

    private String codeKey(String email) {
        return CODE_KEY_PREFIX + email;
    }

    private String verifiedKey(String email) {
        return VERIFIED_KEY_PREFIX + email;
    }
}