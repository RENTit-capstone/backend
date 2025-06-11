package com.capstone.rentit.notification.controller;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public CommonResponse<?> list(Pageable pageable, @Login MemberDto memberDto) {
        return CommonResponse.success(notificationService.findByTarget(memberDto, pageable));
    }

    @PutMapping("/{id}/read")
    public CommonResponse<?> markAsRead(@PathVariable Long id, @Login MemberDto memberDto) {
        notificationService.markAsRead(id, memberDto);
        return CommonResponse.success(null);
    }
}