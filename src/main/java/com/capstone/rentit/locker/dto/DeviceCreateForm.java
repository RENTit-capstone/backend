package com.capstone.rentit.locker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class DeviceCreateForm {
    private Long deviceId;
    private String university;
    private String locationDescription;
}
