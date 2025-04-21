package com.capstone.rentit.register.service;

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

    public boolean checkUniversity(String univName) {
        String url = "https://univcert.com/api/v1/check";
        Map<String, String> request = new HashMap<>();
        request.put("univName", univName);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            return body != null && (boolean) body.getOrDefault("success", false);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean certify(String email, String univName, boolean univCheck) {
        String url = "https://univcert.com/api/v1/certify";
        Map<String, Object> request = new HashMap<>();
        request.put("key", apiKey);
        request.put("email", email);
        request.put("univName", univName);
        request.put("univ_check", univCheck);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            return body != null && (boolean) body.getOrDefault("success", false);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean certifyCode(String email, String univName, int code) {
        String url = "https://univcert.com/api/v1/certifycode";
        Map<String, Object> request = new HashMap<>();
        request.put("key", apiKey);
        request.put("email", email);
        request.put("univName", univName);
        request.put("code", code);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            return body != null && (boolean) body.getOrDefault("success", false);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isCertified(String email) {
        String url = "https://univcert.com/api/v1/status";
        Map<String, Object> request = new HashMap<>();
        request.put("key", apiKey);
        request.put("email", email);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            return body != null && (boolean) body.getOrDefault("success", false);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean clearAll(){
        String url = "https://univcert.com/api/v1/clear";
        Map<String, Object> request = new HashMap<>();
        request.put("key", apiKey);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            return body != null && (boolean) body.getOrDefault("success", false);
        } catch (Exception e) {
            return false;
        }
    }

    public Object showAll(){
        String url = "https://univcert.com/api/v1/certifiedlist";
        Map<String, Object> request = new HashMap<>();
        request.put("key", apiKey);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            if(body != null && (boolean) body.getOrDefault("success", false))
                return body.getOrDefault("data", "empty");
            return "error";
        } catch (Exception e) {
            return "error";
        }
    }
}