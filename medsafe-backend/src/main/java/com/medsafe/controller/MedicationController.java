package com.medsafe.controller;

import com.medsafe.dto.DuplicateWarningResponse;
import com.medsafe.dto.MedicationRequest;
import com.medsafe.dto.MedicationResponse;
import com.medsafe.service.DuplicateDetectionService;
import com.medsafe.service.MedicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationService medicationService;
    private final DuplicateDetectionService duplicateDetectionService;

    public MedicationController(MedicationService medicationService, DuplicateDetectionService duplicateDetectionService) {
        this.medicationService = medicationService;
        this.duplicateDetectionService = duplicateDetectionService;
    }

    @GetMapping
    public ResponseEntity<List<MedicationResponse>> getAllMedications() {
        List<MedicationResponse> medications = medicationService.getAllMedicationsForCurrentUser();
        return ResponseEntity.ok(medications);
    }

    @PostMapping
    public ResponseEntity<MedicationResponse> addMedication(@Valid @RequestBody MedicationRequest request) {
        MedicationResponse created = medicationService.createMedication(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicationResponse> getMedicationById(@PathVariable("id") Long id) {
        MedicationResponse medication = medicationService.getMedicationById(id);
        return ResponseEntity.ok(medication);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicationResponse> updateMedication(@PathVariable("id") Long id,
                                                               @Valid @RequestBody MedicationRequest request) {
        MedicationResponse updated = medicationService.updateMedication(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMedication(@PathVariable("id") Long id) {
        medicationService.deleteMedication(id);
        return ResponseEntity.ok(Map.of("message", "Medication deleted successfully"));
    }

    @GetMapping("/duplicates")
    public ResponseEntity<List<DuplicateWarningResponse>> checkDuplicateMedications() {
        List<DuplicateWarningResponse> duplicates = duplicateDetectionService.detectDuplicateMedicationsForCurrentUser();
        return ResponseEntity.ok(duplicates);
    }
}
