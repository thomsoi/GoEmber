package com.goember.hackathon.stamp;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportStampRepository extends JpaRepository<PassportStamp, Long> {

    long countByStamp_LocationId(Long locationId);

    long countByStamp_StampKey(String stampKey);
}