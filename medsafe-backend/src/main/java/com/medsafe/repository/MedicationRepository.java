package com.medsafe.repository;

import com.medsafe.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {
    List<Medication> findByUserUserId(Long userId);
    List<Medication> findByUserUserIdAndStatus(Long userId, String status);
    List<Medication> findByUserUserIdAndActiveIngredientIgnoreCase(Long userId, String activeIngredient);
}
