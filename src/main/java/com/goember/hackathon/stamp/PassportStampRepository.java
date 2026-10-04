package com.goember.hackathon.stamp;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportStampRepository extends JpaRepository<PassportStamp, Long> {

    long countByStamp_LocationId(Long locationId);

    long countByStamp_StampKey(String stampKey);

    @EntityGraph(attributePaths = "stamp")
    List<PassportStamp> findAllByPassport_PassportId(Long passportId);
}
