package com.goember.hackathon.passport;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportRepository extends JpaRepository<Passport, UUID> {

    Optional<Passport> findByUser_Id(Long userId);
}