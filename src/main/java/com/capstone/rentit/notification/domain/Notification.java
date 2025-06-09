package com.capstone.rentit.notification.domain;

import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.notification.type.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "notification",
        indexes = {
                @Index(name = "idx_notification_target", columnList = "target_id"),
                @Index(name = "idx_notification_target_createdAt", columnList = "target_id, createdAt")
        }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    private Member target;                // 알림 대상 회원

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(nullable = false, length = 255)
    private String body;

    @Column(nullable = false)
    private boolean isRead;                 // 읽음 여부

    private LocalDateTime createdAt;

    public void updateRead(boolean read){
        this.isRead = read;
    }
}
