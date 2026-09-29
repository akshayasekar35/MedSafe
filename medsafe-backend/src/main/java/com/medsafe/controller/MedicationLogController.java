package com.medsafe.controller;

import com.medsafe.dto.MedicationLogRequest;
import com.medsafe.dto.MedicationLogResponse;
import com.medsafe.service.MedicationLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class MedicationLogController {

    private final MedicationLogService medicationLogService;

    public MedicationLogController(MedicationLogService medicationLogService) {
        this.medicationLogService = medicationLogService;
    }

    @PostMapping("/{scheduleId}/taken")
    public ResponseEntity<MedicationLogResponse> markTaken(@PathVariable("scheduleId") Long scheduleId,
                                                          @RequestBody(required = false) MedicationLogRequest request) {
        MedicationLogResponse response = medicationLogService.markTaken(scheduleId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{scheduleId}/missed")
    public ResponseEntity<MedicationLogResponse> markMissed(@PathVariable("scheduleId") Long scheduleId,
                                                           @RequestBody(required = false) MedicationLogRequest request) {
        MedicationLogResponse response = medicationLogService.markMissed(scheduleId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<MedicationLogResponse>> getLogs() {
        List<MedicationLogResponse> logs = medicationLogService.getLogsForCurrentUser();
        return ResponseEntity.ok(logs);
    }
}
