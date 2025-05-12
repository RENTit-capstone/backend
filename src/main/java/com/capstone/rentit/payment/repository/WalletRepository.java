package com.capstone.rentit.payment.repository;

import com.capstone.rentit.payment.domain.Wallet;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.memberId = :memberId")
    Wallet findForUpdate(Long memberId);
}