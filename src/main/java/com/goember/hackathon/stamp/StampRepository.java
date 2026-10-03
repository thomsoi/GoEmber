package com.goember.hackathon.stamp;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StampRepository extends JpaRepository<Stamp, UUID> {

	Optional<Stamp> findByLocationId(Long locationId);
}
