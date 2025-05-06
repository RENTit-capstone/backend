package com.capstone.rentit.locker.dto;

import com.capstone.rentit.locker.domain.Locker;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class LockerDto {
    Long   lockerId;
    boolean available;
    String university;
    String locationDescription;
    LocalDateTime activatedAt;

    public static LockerDto fromEntity(Locker entity) {
        return LockerDto.builder()
                .lockerId(entity.getLockerId())
                .available(entity.isAvailable())
                .university(entity.getUniversity())
                .locationDescription(entity.getLocationDescription())
                .activatedAt(entity.getActivatedAt())
                .build();
    }
}
