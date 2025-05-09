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
public class DeviceResponse {
    Long deviceId;
    private String university;
    private String locationDescription;

    public static DeviceResponse fromEntity(Device entity){
        return DeviceResponse.builder()
                .deviceId(entity.getDeviceId())
                .university(entity.getUniversity())
                .locationDescription(entity.getLocationDescription())
                .build();
    }
}
