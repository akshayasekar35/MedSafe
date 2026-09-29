package com.medsafe.repository;

import com.medsafe.entity.MedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationLogRepository extends JpaRepository<MedicationLog, Long> {
    List<MedicationLog> findByScheduleScheduleId(Long scheduleId);
    List<MedicationLog> findByScheduleMedicationUserUserId(Long userId);
}
