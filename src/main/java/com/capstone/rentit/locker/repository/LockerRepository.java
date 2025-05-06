package com.capstone.rentit.locker.repository;

import com.capstone.rentit.locker.domain.Locker;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LockerRepository extends JpaRepository<Locker, Long>, CustomLockerRepository { }

