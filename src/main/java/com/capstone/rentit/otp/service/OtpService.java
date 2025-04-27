package com.capstone.rentit.otp.service;

import com.capstone.rentit.otp.dto.OtpDto;
import com.capstone.rentit.otp.exception.OtpExpiredException;
import com.capstone.rentit.otp.exception.OtpMismatchException;
import com.capstone.rentit.otp.exception.OtpNotFoundException;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OtpService {

    private static final int OTP_LENGTH = 5;
    private static final Duration OTP_TTL = Duration.ofMinutes(1);

    //in-memory
    private final Map<String, OtpDto> store = new ConcurrentHashMap<>();

    public String generateOtp(String identifier) {
        String otp = buildNumericOtp(OTP_LENGTH);
        Instant expiresAt = Instant.now().plus(OTP_TTL);
        store.put(identifier, new OtpDto(otp, expiresAt));
        return otp;
    }

    public void validateOtp(String identifier, String code)
            throws OtpNotFoundException, OtpExpiredException, OtpMismatchException {

        OtpDto otpDto = store.get(identifier);
        if (otpDto == null) {
            throw new OtpNotFoundException("해당 OTP 를 찾을 수 없습니다.");
        }
        if (Instant.now().isAfter(otpDto.getExpiresAt())) {
            store.remove(identifier);
            throw new OtpExpiredException("OTP 유효시간이 만료되었습니다.");
        }
        if (!otpDto.getCode().equals(code)) {
            throw new OtpMismatchException("입력한 OTP 코드가 일치하지 않습니다.");
        }
        store.remove(identifier);
    }

    private String buildNumericOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(rnd.nextInt(0, 10));
        }
        return sb.toString();
    }
}
