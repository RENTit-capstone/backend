package com.capstone.rentit.register.service;

import com.capstone.rentit.register.exception.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UnivCertService {
    @Value("${univcert.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private static final String BASE = "https://univcert.com/api/v1";

    public void validateUniversity(String univName) {
        ApiResponse resp = post("/check", Map.of("univName", univName));
        if (!resp.isSuccess()) {
            throw new InvalidUniversityException();
        }
    }

    public void sendCertification(String email, String univName, boolean univCheck) {
        ApiResponse resp = post("/certify", Map.of(
                "key", apiKey,
                "email", email,
                "univName", univName,
                "univ_check", univCheck
        ));
        if (!resp.isSuccess()) {
            throw new CertificationSendFailureException();
        }
    }

    public void verifyCode(String email, String univName, int code) {
        ApiResponse resp = post("/certifycode", Map.of(
                "key", apiKey,
                "email", email,
                "univName", univName,
                "code", code
        ));
        if (!resp.isSuccess()) {
            throw new InvalidVerificationCodeException();
        }
    }

    public void ensureCertified(String email) {
        ApiResponse resp = post("/status", Map.of(
                "key", apiKey,
                "email", email
        ));
        if (!resp.isSuccess()) {
            throw new UnivNotCertifiedException();
        }
    }

    public void clearAll() {
        ApiResponse resp = post("/clear", Map.of("key", apiKey));
        if (!resp.isSuccess()) {
            throw new UnivServiceException("인증 데이터 초기화에 실패했습니다.");
        }
    }

    public Object showAll() {
        ApiResponse resp = post("/certifiedlist", Map.of("key", apiKey));
        if (!resp.isSuccess()) {
            throw new UnivServiceException("인증 정보 조회에 실패했습니다.");
        }
        return resp.getData();
    }

    private ApiResponse post(String path, Object request) {
        try {
            ResponseEntity<Map> r = restTemplate.postForEntity(BASE + path, request, Map.class);
            Map<String, Object> body = r.getBody();
            if (body == null) {
                throw new UnivServiceException("외부 인증 서비스 응답이 비어 있습니다.");
            }
            boolean success = (boolean) body.getOrDefault("success", false);
            Object data   = body.get("data");
            return new ApiResponse(success, data);

        } catch (UnivServiceException e) {
            throw e;  // 이미 UnivServiceException
        } catch (Exception e) {
            throw new UnivServiceException("외부 인증 서비스 호출 중 오류가 발생했습니다.", e);
        }
    }

    @Getter
    private static class ApiResponse {
        private final boolean success;
        private final Object data;
        ApiResponse(boolean success, Object data) {
            this.success = success;
            this.data    = data;
        }
    }
}