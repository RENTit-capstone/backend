package com.capstone.rentit.rental.dto;

import com.capstone.rentit.rental.status.RentalStatusEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RentalSearchForm {
    private List<RentalStatusEnum> statuses;
}
