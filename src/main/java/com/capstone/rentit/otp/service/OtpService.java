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
        String otp;
        do { // 충돌 방지
            otp = buildNumericOtp(OTP_LENGTH);
        } while (store.containsKey(otp));

        store.put(otp, new OtpDto(identifier, Instant.now().plus(OTP_TTL)));
        return otp;
    }

    public String validateAndResolveIdentifier(String code)
            throws OtpNotFoundException, OtpExpiredException {

        OtpDto dto = store.get(code);
        if (dto == null) throw new OtpNotFoundException("OTP 를 찾을 수 없습니다.");
        if (Instant.now().isAfter(dto.getExpiresAt())) {
            store.remove(code);
            throw new OtpExpiredException("OTP 유효시간이 만료되었습니다.");
        }
        store.remove(code);
        return dto.getIdentifier();
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
