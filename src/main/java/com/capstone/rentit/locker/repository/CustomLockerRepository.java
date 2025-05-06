package com.capstone.rentit.locker.repository;

import com.capstone.rentit.locker.domain.Locker;
import com.capstone.rentit.locker.dto.LockerSearchForm;
import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.rental.domain.Rental;

import java.util.List;

public interface CustomLockerRepository {
    List<Locker> search(LockerSearchForm form);
}
