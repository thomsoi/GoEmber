package com.goember.hackathon.passport;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportVisitRepository extends JpaRepository<PassportVisit, Long> {
    java.util.List<PassportVisit> findAllByPassport_PassportId(Long passportId);
    Optional<PassportVisit> findByPassport_PassportIdAndOperationId(Long passportId, String operationId);
}
