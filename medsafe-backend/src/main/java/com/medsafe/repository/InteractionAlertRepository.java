package com.medsafe.repository;

import com.medsafe.entity.InteractionAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InteractionAlertRepository extends JpaRepository<InteractionAlert, Long> {
    List<InteractionAlert> findByMedication1MedicationIdOrMedication2MedicationId(Long medicationId1, Long medicationId2);

    @Query("SELECT a FROM InteractionAlert a WHERE a.medication1.user.userId = :userId OR a.medication2.user.userId = :userId")
    List<InteractionAlert> findByUserId(@Param("userId") Long userId);
}
