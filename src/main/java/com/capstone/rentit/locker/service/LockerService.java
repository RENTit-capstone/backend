package com.capstone.rentit.locker.service;

import com.capstone.rentit.locker.domain.Locker;
import com.capstone.rentit.locker.dto.LockerCreateForm;
import com.capstone.rentit.locker.dto.LockerDto;
import com.capstone.rentit.locker.dto.LockerSearchForm;
import com.capstone.rentit.locker.repository.LockerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LockerService {

    private final LockerRepository lockerRepository;

    public Long registerLocker(LockerCreateForm form) {
        return lockerRepository.save(Locker.createLocker(form)).getLockerId();
    }

    @Transactional(readOnly = true)
    public LockerDto getLocker(Long id) {
        return LockerDto.fromEntity(findLocker(id));
    }

    @Transactional(readOnly = true)
    public List<LockerDto> searchLockers(LockerSearchForm form) {
        return lockerRepository.search(form)
                .stream().map(LockerDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<LockerDto> findAvailableLockers(String university) {
        LockerSearchForm form = new LockerSearchForm(university, true);
        List<Locker> list = lockerRepository.search(form);
        return list.stream().map(LockerDto::fromEntity).toList();
    }

    private Locker findLocker(Long id) {
        return lockerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사물함이 존재하지 않습니다. id=" + id));
    }
}
