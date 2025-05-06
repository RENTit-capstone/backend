package com.capstone.rentit.member.repository;

import com.capstone.rentit.member.domain.Member;

import java.util.Optional;

public interface CustomMemberRepository {
    Optional<Member> findProfileWithAll(Long memberId);
}
