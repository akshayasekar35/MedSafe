package com.medsafe.service;

import com.medsafe.dto.MedicationLogRequest;
import com.medsafe.dto.MedicationLogResponse;
import com.medsafe.entity.MedicationLog;
import com.medsafe.entity.Schedule;
import com.medsafe.repository.MedicationLogRepository;
import com.medsafe.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicationLogService {

    private final MedicationLogRepository medicationLogRepository;
    private final ScheduleService scheduleService;
    private final SecurityUtils securityUtils;

    public MedicationLogService(MedicationLogRepository medicationLogRepository, ScheduleService scheduleService, SecurityUtils securityUtils) {
        this.medicationLogRepository = medicationLogRepository;
        this.scheduleService = scheduleService;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public MedicationLogResponse markTaken(Long scheduleId, MedicationLogRequest request) {
        Schedule schedule = scheduleService.getScheduleEntityForCurrentUser(scheduleId);

        LocalDateTime scheduledDateTime = resolveScheduledDateTime(schedule, request);
        LocalDateTime actualDateTime = LocalDateTime.now();

        MedicationLog log = new MedicationLog(schedule, scheduledDateTime, actualDateTime, "TAKEN");
        MedicationLog saved = medicationLogRepository.save(log);
        return mapToResponse(saved);
    }

    @Transactional
    public MedicationLogResponse markMissed(Long scheduleId, MedicationLogRequest request) {
        Schedule schedule = scheduleService.getScheduleEntityForCurrentUser(scheduleId);

        LocalDateTime scheduledDateTime = resolveScheduledDateTime(schedule, request);

        MedicationLog log = new MedicationLog(schedule, scheduledDateTime, null, "MISSED");
        MedicationLog saved = medicationLogRepository.save(log);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MedicationLogResponse> getLogsForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        return medicationLogRepository.findByScheduleMedicationUserUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private LocalDateTime resolveScheduledDateTime(Schedule schedule, MedicationLogRequest request) {
        if (request != null && request.getScheduledDatetime() != null) {
            return request.getScheduledDatetime();
        }
        return LocalDateTime.of(LocalDate.now(), schedule.getScheduledTime());
    }

    public MedicationLogResponse mapToResponse(MedicationLog log) {
        return new MedicationLogResponse(
                log.getLogId(),
                log.getSchedule().getScheduleId(),
                log.getSchedule().getMedication().getMedicationId(),
                log.getSchedule().getMedication().getMedicineName(),
                log.getSchedule().getScheduledTime(),
                log.getSchedule().getMealRelation(),
                log.getScheduledDatetime(),
                log.getActualDatetime(),
                log.getStatus()
        );
    }
}
