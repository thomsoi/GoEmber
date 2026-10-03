package com.goember.hackathon.passport;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportJourneyRepository extends JpaRepository<PassportJourney, Long> {

    Optional<PassportJourney> findByPassport_PassportIdAndJourneyKey(Long passportId, String journeyKey);

    List<PassportJourney> findAllByPassport_PassportId(Long passportId);
}
