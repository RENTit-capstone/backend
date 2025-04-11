package com.capstone.rentit.register.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class UnivCertServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UnivCertService univCertService;

    @BeforeEach
    public void setUp() {
        // apiKey는 application.yml 대신 테스트 환경에서 임의의 값("dummy-key")으로 설정
        ReflectionTestUtils.setField(univCertService, "apiKey", "dummy-key");
    }

    @Test
    public void testCheckUniversity_success() {
        // given
        String univName = "Test University";
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // when
        boolean result = univCertService.checkUniversity(univName);

        // then
        assertTrue(result);
    }

    @Test
    public void testCheckUniversity_failure() {
        // given
        String univName = "Test University";
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // when
        boolean result = univCertService.checkUniversity(univName);

        // then
        assertFalse(result);
    }

    @Test
    public void testCertify_success() {
        // given
        String email = "user@test.com";
        String univ = "Test University";
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // when
        boolean result = univCertService.certify(email, univ, false);

        // then
        assertTrue(result);
    }

    @Test
    public void testCertify_failure() {
        // given
        String email = "user@test.com";
        String univ = "Test University";
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // when
        boolean result = univCertService.certify(email, univ, false);

        // then
        assertFalse(result);
    }

    @Test
    public void testCertifyCode_success() {
        // given
        String email = "user@test.com";
        String univ = "Test University";
        int code = 123456;
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // when
        boolean result = univCertService.certifyCode(email, univ, code);

        // then
        assertTrue(result);
    }

    @Test
    public void testCertifyCode_failure() {
        // given
        String email = "user@test.com";
        String univ = "Test University";
        int code = 123456;
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // when
        boolean result = univCertService.certifyCode(email, univ, code);

        // then
        assertFalse(result);
    }

    @Test
    public void testIsCertified_success() {
        // given
        String email = "user@test.com";
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // when
        boolean result = univCertService.isCertified(email);

        // then
        assertTrue(result);
    }

    @Test
    public void testIsCertified_failure() {
        // given
        String email = "user@test.com";
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // when
        boolean result = univCertService.isCertified(email);

        // then
        assertFalse(result);
    }

    @Test
    public void testClearAll_success() {
        // given
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // when
        boolean result = univCertService.clearAll();

        // then
        assertTrue(result);
    }

    @Test
    public void testClearAll_failure() {
        // given
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // when
        boolean result = univCertService.clearAll();

        // then
        assertFalse(result);
    }

    @Test
    public void testShowAll_success() {
        // given
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("data", data);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // when
        Object result = univCertService.showAll();

        // then
        assertEquals(data, result);
    }

    @Test
    public void testShowAll_failure() {
        // given
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(responseEntity);

        // when
        Object result = univCertService.showAll();

        // then
        assertEquals("error", result);
    }
}