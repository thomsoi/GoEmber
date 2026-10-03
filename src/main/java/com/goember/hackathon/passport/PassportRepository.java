package com.goember.hackathon.passport;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportRepository extends JpaRepository<Passport, UUID> {

    @EntityGraph(attributePaths = {"stamps", "stamps.stamp"})
    Optional<Passport> findByUser_Id(Long userId);
}