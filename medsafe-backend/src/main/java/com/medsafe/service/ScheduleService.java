package com.medsafe.service;

import com.medsafe.dto.ScheduleRequest;
import com.medsafe.dto.ScheduleResponse;
import com.medsafe.entity.Medication;
import com.medsafe.entity.Schedule;
import com.medsafe.exception.ResourceNotFoundException;
import com.medsafe.exception.UnauthorizedException;
import com.medsafe.repository.ScheduleRepository;
import com.medsafe.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final MedicationService medicationService;
    private final SecurityUtils securityUtils;

    public ScheduleService(ScheduleRepository scheduleRepository, MedicationService medicationService, SecurityUtils securityUtils) {
        this.scheduleRepository = scheduleRepository;
        this.medicationService = medicationService;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedulesByMedicationId(Long medicationId) {
        // Enforce user authorization on medication
        Medication medication = medicationService.getMedicationEntityForCurrentUser(medicationId);
        return scheduleRepository.findByMedicationMedicationId(medication.getMedicationId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ScheduleResponse createSchedule(Long medicationId, ScheduleRequest request) {
        Medication medication = medicationService.getMedicationEntityForCurrentUser(medicationId);

        Schedule schedule = new Schedule();
        schedule.setMedication(medication);
        schedule.setScheduledTime(request.getScheduledTime());
        schedule.setMealRelation(request.getMealRelation() != null ? request.getMealRelation().trim() : null);

        Schedule saved = scheduleRepository.save(schedule);
        return mapToResponse(saved);
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long scheduleId, ScheduleRequest request) {
        Schedule schedule = getScheduleEntityForCurrentUser(scheduleId);

        schedule.setScheduledTime(request.getScheduledTime());
        if (request.getMealRelation() != null) {
            schedule.setMealRelation(request.getMealRelation().trim());
        }

        Schedule updated = scheduleRepository.save(schedule);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        Schedule schedule = getScheduleEntityForCurrentUser(scheduleId);
        scheduleRepository.delete(schedule);
    }

    public Schedule getScheduleEntityForCurrentUser(Long scheduleId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule with ID " + scheduleId + " not found."));

        if (!schedule.getMedication().getUser().getUserId().equals(currentUserId)) {
            throw new UnauthorizedException("You are not authorized to access this schedule.");
        }
        return schedule;
    }

    public ScheduleResponse mapToResponse(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getScheduleId(),
                schedule.getMedication().getMedicationId(),
                schedule.getMedication().getMedicineName(),
                schedule.getScheduledTime(),
                schedule.getMealRelation()
        );
    }
}
