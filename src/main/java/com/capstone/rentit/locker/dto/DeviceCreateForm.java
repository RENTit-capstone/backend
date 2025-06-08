package com.capstone.rentit.locker.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class DeviceCreateForm {
    @NotNull private Long deviceId;
    @NotEmpty private String university;
    @NotEmpty private String locationDescription;
}
