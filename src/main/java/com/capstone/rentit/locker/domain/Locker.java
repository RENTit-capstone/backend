package com.capstone.rentit.locker.domain;

import com.capstone.rentit.locker.dto.LockerCreateForm;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(DeviceLockerId.class)          // 복합키 (deviceId, lockerId)
public class Locker {

    @Id
    private Long deviceId;

    @Id
    private Long lockerId;              // 칸 번호 (1~)

    private boolean available;
    private LocalDateTime activatedAt;

    /* 연관 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deviceId", insertable = false, updatable = false)
    private Device device;

    public void changeAvailability(boolean avail) { this.available = avail; }

    public static Locker createLocker(LockerCreateForm form, Long lockerId){
        return Locker.builder()
                .deviceId(form.getDeviceId())
                .lockerId(lockerId)
                .available(true)
                .activatedAt(LocalDateTime.now())
                .build();
    }
}
