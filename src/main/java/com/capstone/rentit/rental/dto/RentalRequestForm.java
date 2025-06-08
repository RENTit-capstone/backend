package com.capstone.rentit.rental.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RentalRequestForm {
    @NotNull private Long itemId;
    @NotNull private Long ownerId;
    @NotNull private Long renterId;
    @NotNull private LocalDateTime startDate;  // 예정 대여일
    @NotNull private LocalDateTime dueDate;    // 반납 예정일
}
