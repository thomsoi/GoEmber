package com.goember.hackathon.journey;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JourneyRepository extends JpaRepository<Journey, Long> {

    Optional<Journey> findByUserIdAndActiveTrue(Long userId);
}
