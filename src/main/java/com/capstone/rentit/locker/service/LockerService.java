package com.capstone.rentit.locker.service;

import com.capstone.rentit.locker.domain.Device;
import com.capstone.rentit.locker.domain.Locker;
import com.capstone.rentit.locker.dto.*;
import com.capstone.rentit.locker.exception.LockerNotFoundException;
import com.capstone.rentit.locker.repository.DeviceRepository;
import com.capstone.rentit.locker.repository.LockerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LockerService {

    private final DeviceRepository deviceRepository;
    private final LockerRepository lockerRepository;

    public Long registerDevice(DeviceCreateForm form) {
        return deviceRepository.save(Device.createDevice(form)).getDeviceId();
    }

    public Long registerLocker(LockerCreateForm form) {
        Long maxId = lockerRepository.findMaxLockerIdByDeviceId(form.getDeviceId())
                .orElse(0L);
        Long nextId = maxId + 1;

        return lockerRepository.save(Locker.createLocker(form, nextId)).getLockerId();
    }

    public List<DeviceResponse> searchAll() {
        return deviceRepository.findAll()
                .stream().map(DeviceResponse::fromEntity).toList();
    }

    public List<DeviceResponse> searchDevicesByUniversity(DeviceSearchForm form) {
        return deviceRepository.findByUniversity(form.getUniversity())
                .stream().map(DeviceResponse::fromEntity).toList();
    }

    public List<LockerResponse> searchLockers(LockerSearchForm form) {
        return lockerRepository.search(form)
                .stream().map(LockerResponse::fromEntity).toList();
    }

    public List<LockerBriefResponse> findAvailableLockers(Long deviceId) {
        LockerSearchForm form = new LockerSearchForm(deviceId, true);
        List<Locker> list = lockerRepository.search(form);
        return list.stream()
                .map(l -> new LockerBriefResponse(l.getDeviceId(), l.getLockerId(), l.isAvailable()))
                .toList();
    }
}
