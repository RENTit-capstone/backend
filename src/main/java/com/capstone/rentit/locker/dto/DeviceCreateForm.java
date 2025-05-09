package com.capstone.rentit.locker.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DeviceCreateForm {
    private String university;
    private String locationDescription;
}
