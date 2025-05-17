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
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member target;                // 알림 대상 회원

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(nullable = false, length = 255)
    private String body;

    @Column(nullable = false)
    private boolean read;                 // 읽음 여부

    private LocalDateTime createdAt;

    public void updateRead(boolean read){
        this.read = read;
    }
}
