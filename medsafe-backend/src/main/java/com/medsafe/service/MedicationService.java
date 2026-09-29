package com.medsafe.service;

import com.medsafe.dto.MedicationRequest;
import com.medsafe.dto.MedicationResponse;
import com.medsafe.entity.Medication;
import com.medsafe.entity.User;
import com.medsafe.exception.BadRequestException;
import com.medsafe.exception.ResourceNotFoundException;
import com.medsafe.exception.UnauthorizedException;
import com.medsafe.repository.MedicationRepository;
import com.medsafe.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final SecurityUtils securityUtils;

    public MedicationService(MedicationRepository medicationRepository, SecurityUtils securityUtils) {
        this.medicationRepository = medicationRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public List<MedicationResponse> getAllMedicationsForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        return medicationRepository.findByUserUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MedicationResponse getMedicationById(Long medicationId) {
        Medication medication = getMedicationEntityForCurrentUser(medicationId);
        return mapToResponse(medication);
    }

    @Transactional
    public MedicationResponse createMedication(MedicationRequest request) {
        validateMedicationRequest(request);
        User currentUser = securityUtils.getCurrentUser();

        Medication medication = new Medication();
        medication.setUser(currentUser);
        medication.setMedicineName(request.getMedicineName().trim());
        medication.setActiveIngredient(request.getActiveIngredient() != null && !request.getActiveIngredient().isBlank() ? request.getActiveIngredient().trim() : null);
        medication.setDosage(request.getDosage() != null && !request.getDosage().isBlank() ? request.getDosage().trim() : null);
        medication.setFrequency(request.getFrequency() != null && !request.getFrequency().isBlank() ? request.getFrequency().trim() : null);
        medication.setStartDate(request.getStartDate());
        medication.setEndDate(request.getEndDate());
        medication.setStatus(request.getStatus() != null && !request.getStatus().isBlank() ? request.getStatus().trim().toUpperCase() : "ACTIVE");

        Medication saved = medicationRepository.save(medication);
        return mapToResponse(saved);
    }

    @Transactional
    public MedicationResponse updateMedication(Long medicationId, MedicationRequest request) {
        validateMedicationRequest(request);
        Medication medication = getMedicationEntityForCurrentUser(medicationId);

        medication.setMedicineName(request.getMedicineName().trim());
        medication.setActiveIngredient(request.getActiveIngredient() != null && !request.getActiveIngredient().isBlank() ? request.getActiveIngredient().trim() : null);
        medication.setDosage(request.getDosage() != null && !request.getDosage().isBlank() ? request.getDosage().trim() : null);
        medication.setFrequency(request.getFrequency() != null && !request.getFrequency().isBlank() ? request.getFrequency().trim() : null);
        medication.setStartDate(request.getStartDate());
        medication.setEndDate(request.getEndDate());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            medication.setStatus(request.getStatus().trim().toUpperCase());
        }

        Medication updated = medicationRepository.save(medication);
        return mapToResponse(updated);
    }

    private void validateMedicationRequest(MedicationRequest request) {
        if (request == null) {
            throw new BadRequestException("Medication request cannot be empty.");
        }
        if (request.getMedicineName() == null || request.getMedicineName().trim().isBlank()) {
            throw new BadRequestException("Medicine name is required and cannot be blank.");
        }
        if (request.getStartDate() != null && request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Medication end date cannot be before start date.");
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String s = request.getStatus().trim().toUpperCase();
            if (!s.equals("ACTIVE") && !s.equals("INACTIVE")) {
                throw new BadRequestException("Medication status must be either ACTIVE or INACTIVE.");
            }
        }
    }

    @Transactional
    public void deleteMedication(Long medicationId) {
        Medication medication = getMedicationEntityForCurrentUser(medicationId);
        medicationRepository.delete(medication);
    }

    public Medication getMedicationEntityForCurrentUser(Long medicationId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Medication medication = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Medication with ID " + medicationId + " not found."));

        if (!medication.getUser().getUserId().equals(currentUserId)) {
            throw new UnauthorizedException("You are not authorized to access this medication.");
        }
        return medication;
    }

    public MedicationResponse mapToResponse(Medication medication) {
        return new MedicationResponse(
                medication.getMedicationId(),
                medication.getUser().getUserId(),
                medication.getMedicineName(),
                medication.getActiveIngredient(),
                medication.getDosage(),
                medication.getFrequency(),
                medication.getStartDate(),
                medication.getEndDate(),
                medication.getStatus(),
                medication.getCreatedAt()
        );
    }
}
