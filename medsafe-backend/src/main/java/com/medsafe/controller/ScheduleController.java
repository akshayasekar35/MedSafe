package com.medsafe.controller;

import com.medsafe.dto.ScheduleRequest;
import com.medsafe.dto.ScheduleResponse;
import com.medsafe.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/api/medications/{id}/schedules")
    public ResponseEntity<ScheduleResponse> addSchedule(@PathVariable("id") Long medicationId,
                                                        @Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse created = scheduleService.createSchedule(medicationId, request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/api/medications/{id}/schedules")
    public ResponseEntity<List<ScheduleResponse>> getSchedulesForMedication(@PathVariable("id") Long medicationId) {
        List<ScheduleResponse> schedules = scheduleService.getSchedulesByMedicationId(medicationId);
        return ResponseEntity.ok(schedules);
    }

    @PutMapping("/api/schedules/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(@PathVariable("id") Long scheduleId,
                                                           @Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse updated = scheduleService.updateSchedule(scheduleId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/api/schedules/{id}")
    public ResponseEntity<Map<String, String>> deleteSchedule(@PathVariable("id") Long scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.ok(Map.of("message", "Schedule deleted successfully"));
    }
}
