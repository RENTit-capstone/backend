package com.capstone.rentit.member.repository;

import com.capstone.rentit.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, CustomMemberRepository {
    Optional<Member> findByEmail(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Member m set m.fcmToken = :token where m.memberId = :memberId")
    void updateFcmToken(@Param("memberId") Long memberId,
                       @Param("token") String token);
}

