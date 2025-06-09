package com.capstone.rentit.otp.service;

import com.capstone.rentit.otp.dto.OtpDto;
import com.capstone.rentit.otp.exception.OtpExpiredException;
import com.capstone.rentit.otp.exception.OtpMismatchException;
import com.capstone.rentit.otp.exception.OtpNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_LENGTH = 5;
    private static final Duration OTP_TTL = Duration.ofMinutes(1);

    private final StringRedisTemplate redis;

    public String generateOtp(String identifier) {
        String otp;
        do { // 충돌 방지
            otp = buildNumericOtp(OTP_LENGTH);
        } while (Boolean.TRUE.equals(redis.hasKey(key(otp))));

        redis.opsForValue().set(key(otp), identifier, OTP_TTL);
        return otp;
    }

    public String validateAndResolveIdentifier(String code)
            throws OtpNotFoundException, OtpExpiredException {

        String redisKey = key(code);
        String identifier = redis.opsForValue().get(redisKey);

        if (identifier == null) throw new OtpNotFoundException("OTP 를 찾을 수 없습니다.");

        redis.delete(redisKey);
        return identifier;
    }

    private String buildNumericOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(rnd.nextInt(0, 10));
        }
        return sb.toString();
    }

    private String key(String otp) {
        return "otp:" + otp;   // Redis 키 네임스페이스(prefix) 용도
    }
}
