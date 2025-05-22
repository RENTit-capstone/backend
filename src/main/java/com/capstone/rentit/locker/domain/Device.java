package com.capstone.rentit.locker.domain;

import com.capstone.rentit.locker.dto.DeviceCreateForm;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    @Id
    private Long deviceId;

    @Column(length = 50, nullable = false)
    private String university;

    @Column(length = 100)
    private String locationDescription;

    @OneToMany(
            mappedBy = "device",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<Locker> lockers = new ArrayList<>();

    static public Device createDevice(DeviceCreateForm form){
        return Device.builder()
                .university(form.getUniversity())
                .locationDescription(form.getLocationDescription())
                .build();
    }
}
