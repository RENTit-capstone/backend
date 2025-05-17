package com.capstone.rentit.notification.repository;

import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByTarget(Member target, Pageable pageable);
}
