package com.capstone.rentit.locker.repository;

import com.capstone.rentit.locker.domain.Locker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LockerRepository extends JpaRepository<Locker, Long>, CustomLockerRepository {
    @Query("select max(l.lockerId) from Locker l where l.deviceId = :deviceId")
    Optional<Long> findMaxLockerIdByDeviceId(@Param("deviceId") Long deviceId);
}

