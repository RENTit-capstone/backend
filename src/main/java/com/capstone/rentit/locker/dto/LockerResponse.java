package com.capstone.rentit.locker.dto;

import com.capstone.rentit.locker.domain.Device;
import com.capstone.rentit.locker.domain.Locker;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class LockerResponse {
    Long deviceId;
    Long lockerId;
    boolean available;
    LocalDateTime activatedAt;
    Device device;

    public static LockerResponse fromEntity(Locker entity){
        return LockerResponse.builder()
                .deviceId(entity.getDeviceId())
                .lockerId(entity.getLockerId())
                .available(entity.isAvailable())
                .activatedAt(entity.getActivatedAt())
                .device(entity.getDevice())
                .build();
    }
}
