package com.capstone.rentit.locker.domain;

import com.capstone.rentit.locker.dto.LockerCreateForm;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Locker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lockerId;

    @Column(nullable = false)
    private boolean available;

    @Column(length = 50, nullable = false)
    private String university;

    @Column(length = 100)
    private String locationDescription;

    @Column(nullable = false, updatable = false)
    private LocalDateTime activatedAt;

    public void changeAvailability(boolean available) {
        this.available = available;
    }

    public static Locker createLocker(LockerCreateForm form){
        return Locker.builder()
                .available(true)
                .university(form.getUniversity())
                .locationDescription(form.getLocationDescription())
                .activatedAt(LocalDateTime.now())
                .build();
    }
}
