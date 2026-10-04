package com.goember.hackathon.journey;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface JourneyRepository extends JpaRepository<Journey, Long> {
    @Query("select j.userId from Journey j where j.id = :id")
    Optional<Long> findOwnerId(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Journey j where j.id = :id")
    Optional<Journey> findForUpdate(@Param("id") Long id);

    Optional<Journey> findByUserIdAndActiveTrue(Long userId);
}
