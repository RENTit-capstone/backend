package com.capstone.rentit.rental.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RentalRequestForm {
    private Long itemId;
    private Long ownerId;
    private Long renterId;
    private LocalDateTime startDate;  // 예정 대여일
    private LocalDateTime dueDate;    // 반납 예정일
}
