package com.goember.hackathon.stamp;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StampRepository extends JpaRepository<Stamp, Long> {

	Optional<Stamp> findByLocationId(Long locationId);
}
