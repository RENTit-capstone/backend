package com.capstone.rentit.notification.service;

import com.capstone.rentit.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceTokenService {

    private final MemberRepository memberRepository;

    /**
     * 로그인 직후, 혹은 클라이언트가 새 토큰을 보내 왔을 때 호출
     */
    public void saveToken(Long memberId, String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        memberRepository.updateFcmToken(memberId, token);
    }
}