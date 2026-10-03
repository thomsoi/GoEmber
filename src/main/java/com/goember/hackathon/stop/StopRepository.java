package com.goember.hackathon.stop;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StopRepository extends JpaRepository<Stop, Long> {

    Optional<Stop> findByEmberLocationId(String emberLocationId);

    List<Stop> findByType(String type); // e.g. findByType("STOP_POINT")

    Optional<Stop> findByEmberLocationId(Long emberLocationId);
}