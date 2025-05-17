package com.capstone.rentit.notification.dto;

import com.capstone.rentit.notification.domain.Notification;
import com.capstone.rentit.notification.type.NotificationType;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        NotificationType type,
        String title,
        String body,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}