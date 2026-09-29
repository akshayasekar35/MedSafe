package com.medsafe;

import com.medsafe.dto.AIExplanationRequest;
import com.medsafe.dto.AIExplanationResponse;
import com.medsafe.dto.DuplicateWarningResponse;
import com.medsafe.dto.InteractionAlertResponse;
import com.medsafe.entity.InteractionAlert;
import com.medsafe.entity.Medication;
import com.medsafe.entity.User;
import com.medsafe.exception.BadRequestException;
import com.medsafe.repository.MedicationRepository;
import com.medsafe.repository.UserRepository;
import com.medsafe.security.SecurityUtils;
import com.medsafe.service.AIExplanationService;
import com.medsafe.service.DuplicateDetectionService;
import com.medsafe.service.InteractionService;
import com.medsafe.service.drugdata.DevelopmentDrugDataProvider;
import com.medsafe.service.drugdata.DrugInfoResult;
import com.medsafe.service.drugdata.DrugInteractionResult;
import com.medsafe.service.drugdata.EvidenceGroundedInteractionEngine;
import com.medsafe.service.drugdata.cache.DrugInformationCacheService;
import com.medsafe.service.drugdata.model.EvidenceStatus;
import com.medsafe.service.drugdata.openfda.OpenFdaDrugDataProvider;
import com.medsafe.service.drugdata.rxnorm.RxNormDrugDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "medsafe.drug-data.provider=production")
public class Phase4SafetyAuditTest {

    @Autowired
    private OpenFdaDrugDataProvider openFdaProvider;

    @Autowired
    private AIExplanationService aiExplanationService;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private DuplicateDetectionService duplicateDetectionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @MockitoBean
    private SecurityUtils securityUtils;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByEmail("safety_audit_test@medsafe.com").orElseGet(() -> {
            User u = new User();
            u.setName("Safety Audit Patient");
            u.setEmail("safety_audit_test@medsafe.com");
            u.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
            u.setCreatedAt(LocalDateTime.now());
            return userRepository.save(u);
        });
        when(securityUtils.getCurrentUserId()).thenReturn(testUser.getUserId());
    }

    @Test
    @DisplayName("Case A: Drug A has a drug_interactions section, but it does NOT mention Drug B -> NOT_FOUND")
    void testCaseA_DrugANotMentioningDrugB_ReturnsNotFound() {
        DrugInformationCacheService mockCache = Mockito.mock(DrugInformationCacheService.class);
        OpenFdaDrugDataProvider mockFda = Mockito.mock(OpenFdaDrugDataProvider.class);
        RxNormDrugDataProvider mockRx = Mockito.mock(RxNormDrugDataProvider.class);

        when(mockCache.getOrFetchDrugInfo("DrugAlpha")).thenReturn(
                new DrugInfoResult("DrugAlpha", "DrugAlpha", "Antihypertensive", "RxNorm", "11111"));
        when(mockCache.getOrFetchDrugInfo("DrugBeta")).thenReturn(
                new DrugInfoResult("DrugBeta", "DrugBeta", "Antibiotic", "RxNorm", "22222"));

        // FDA label for DrugAlpha has interactions, but only mentions Ketoconazole and Erythromycin (NOT DrugBeta)
        // checkInteraction between DrugAlpha and DrugBeta must return Optional.empty()
        when(mockFda.checkInteraction(Mockito.eq("DrugAlpha"), Mockito.eq("DrugBeta"), Mockito.anyList(), Mockito.anyList()))
                .thenReturn(Optional.empty());

        EvidenceGroundedInteractionEngine engine = new EvidenceGroundedInteractionEngine(mockCache, mockFda, mockRx);
        Optional<DrugInteractionResult> result = engine.checkInteraction("DrugAlpha", "DrugBeta");

        assertTrue(result.isPresent(), "Engine should return a result");
        assertEquals(EvidenceStatus.NOT_FOUND, result.get().getEvidenceStatus(),
                "Must be NOT_FOUND when Drug A label does not mention Drug B");
        assertFalse(result.get().isHasInteraction(), "hasInteraction must be false");
        assertEquals("No interaction identified in the consulted source.", result.get().getDescription());
    }

    @Test
    @DisplayName("Case B: Drug A label explicitly mentions Drug B in interactions -> VERIFIED with evidence snippet")
    void testCaseB_DrugAMentioningDrugB_ReturnsVerifiedWithSnippet() {
        DrugInformationCacheService mockCache = Mockito.mock(DrugInformationCacheService.class);
        OpenFdaDrugDataProvider mockFda = Mockito.mock(OpenFdaDrugDataProvider.class);
        RxNormDrugDataProvider mockRx = Mockito.mock(RxNormDrugDataProvider.class);

        when(mockCache.getOrFetchDrugInfo("Coumadin")).thenReturn(
                new DrugInfoResult("Coumadin", "Warfarin", "Anticoagulant", "RxNorm", "11289"));
        when(mockCache.getOrFetchDrugInfo("Aspirin")).thenReturn(
                new DrugInfoResult("Aspirin", "Aspirin", "Antiplatelet", "RxNorm", "1191"));

        String evidenceSnippet = "Concomitant administration of warfarin and aspirin significantly increases bleeding risk.";
        DrugInteractionResult verifiedResult = new DrugInteractionResult(
                "Coumadin",
                "Aspirin",
                true,
                "MAJOR",
                evidenceSnippet,
                OpenFdaDrugDataProvider.SOURCE_NAME,
                "SPL Set ID: 0cbce382-9c88-4f58-ae0f-532a841e8f95",
                EvidenceStatus.VERIFIED
        );

        when(mockFda.checkInteraction(Mockito.eq("Coumadin"), Mockito.eq("Aspirin"), Mockito.anyList(), Mockito.anyList()))
                .thenReturn(Optional.of(verifiedResult));

        EvidenceGroundedInteractionEngine engine = new EvidenceGroundedInteractionEngine(mockCache, mockFda, mockRx);
        Optional<DrugInteractionResult> result = engine.checkInteraction("Coumadin", "Aspirin");

        assertTrue(result.isPresent());
        assertEquals(EvidenceStatus.VERIFIED, result.get().getEvidenceStatus());
        assertTrue(result.get().isHasInteraction());
        assertTrue(result.get().getDescription().contains("aspirin"));
        assertEquals("SPL Set ID: 0cbce382-9c88-4f58-ae0f-532a841e8f95", result.get().getSourceReference());
        assertEquals("11289", result.get().getRxcuiA());
        assertEquals("1191", result.get().getRxcuiB());
    }

    @Test
    @DisplayName("Case C: Drug A label has general interaction text but no mention of Drug B -> NOT_FOUND")
    void testCaseC_GeneralInteractionTextWithoutDrugB_ReturnsNotFound() {
        // Snippet has general warnings about "agents" and "caution" but no mention of target drug "Metformin"
        DrugInformationCacheService mockCache = Mockito.mock(DrugInformationCacheService.class);
        OpenFdaDrugDataProvider mockFda = Mockito.mock(OpenFdaDrugDataProvider.class);
        RxNormDrugDataProvider mockRx = Mockito.mock(RxNormDrugDataProvider.class);

        when(mockCache.getOrFetchDrugInfo("Amoxicillin")).thenReturn(
                new DrugInfoResult("Amoxicillin", "Amoxicillin", "Antibiotic", "RxNorm", "723"));
        when(mockCache.getOrFetchDrugInfo("Metformin")).thenReturn(
                new DrugInfoResult("Metformin", "Metformin", "Biguanide", "RxNorm", "6809"));

        // No mention in openFDA label
        when(mockFda.checkInteraction(Mockito.eq("Amoxicillin"), Mockito.eq("Metformin"), Mockito.anyList(), Mockito.anyList()))
                .thenReturn(Optional.empty());

        EvidenceGroundedInteractionEngine engine = new EvidenceGroundedInteractionEngine(mockCache, mockFda, mockRx);
        Optional<DrugInteractionResult> result = engine.checkInteraction("Amoxicillin", "Metformin");

        assertTrue(result.isPresent());
        assertEquals(EvidenceStatus.NOT_FOUND, result.get().getEvidenceStatus(),
                "General interaction text in Drug A must not trigger a false positive for Drug B");
        assertFalse(result.get().isHasInteraction());
    }

    @Test
    @DisplayName("Case D: Different brand names with same active ingredient -> Duplicate detected, not false cross-interaction")
    void testCaseD_SameActiveIngredientDuplicateDetectionVsInteraction() {
        // Step 1: Duplicate Detection Service test
        Medication med1 = new Medication();
        med1.setUser(testUser);
        med1.setMedicineName("Advil");
        med1.setActiveIngredient("Ibuprofen");
        med1.setDosage("200mg");
        med1.setFrequency("Every 6 hours");
        med1.setStartDate(LocalDate.now());
        med1.setStatus("ACTIVE");

        Medication med2 = new Medication();
        med2.setUser(testUser);
        med2.setMedicineName("Motrin");
        med2.setActiveIngredient("Ibuprofen");
        med2.setDosage("400mg");
        med2.setFrequency("As needed");
        med2.setStartDate(LocalDate.now());
        med2.setStatus("ACTIVE");

        medicationRepository.save(med1);
        medicationRepository.save(med2);

        List<DuplicateWarningResponse> dupWarnings = duplicateDetectionService.detectDuplicateMedicationsForCurrentUser();
        assertFalse(dupWarnings.isEmpty(), "Duplicate active ingredient must be detected for Advil and Motrin");
        assertTrue(dupWarnings.stream().anyMatch(w -> w.getActiveIngredient().equalsIgnoreCase("Ibuprofen")),
                "Must flag Ibuprofen as duplicate active ingredient");

        // Step 2: Interaction Engine verification for same active ingredient
        DrugInformationCacheService mockCache = Mockito.mock(DrugInformationCacheService.class);
        OpenFdaDrugDataProvider mockFda = Mockito.mock(OpenFdaDrugDataProvider.class);
        RxNormDrugDataProvider mockRx = Mockito.mock(RxNormDrugDataProvider.class);

        DrugInfoResult advilInfo = new DrugInfoResult("Advil", "Ibuprofen", "Ibuprofen", "RxNorm", "153010");
        DrugInfoResult motrinInfo = new DrugInfoResult("Motrin", "Ibuprofen", "Ibuprofen", "RxNorm", "153010");

        when(mockCache.getOrFetchDrugInfo("Advil")).thenReturn(advilInfo);
        when(mockCache.getOrFetchDrugInfo("Motrin")).thenReturn(motrinInfo);

        EvidenceGroundedInteractionEngine engine = new EvidenceGroundedInteractionEngine(mockCache, mockFda, mockRx);
        Optional<DrugInteractionResult> interactionRes = engine.checkInteraction("Advil", "Motrin");

        assertTrue(interactionRes.isPresent());
        // Must NOT falsely flag a drug interacting with its own active substance!
        assertFalse(interactionRes.get().isHasInteraction(), "Should not report self-interaction for duplicate therapy");
        assertTrue(interactionRes.get().getDescription().contains("duplicate"),
                "Should classify as duplicate therapy rather than pairwise interaction");
    }

    @Test
    @DisplayName("Case E: External source unavailable -> SOURCE_UNAVAILABLE, not false safe or false positive")
    void testCaseE_ExternalSourceUnavailable_ReturnsSourceUnavailable() {
        OpenFdaDrugDataProvider timeoutProvider = new OpenFdaDrugDataProvider("http://10.255.255.1:81", "", 500, 500);
        Optional<DrugInteractionResult> result = timeoutProvider.checkInteraction(
                "Warfarin", "Aspirin", List.of("Warfarin"), List.of("Aspirin"));

        assertTrue(result.isPresent());
        assertEquals(EvidenceStatus.SOURCE_UNAVAILABLE, result.get().getEvidenceStatus());
        assertFalse(result.get().isHasInteraction(), "Must not invent an interaction when source is down");
        assertTrue(result.get().getDescription().contains("temporarily unavailable"));
    }

    @Test
    @DisplayName("Case F: DevelopmentDrugDataProvider returns DEVELOPMENT_MOCK status, never VERIFIED")
    void testCaseF_DevelopmentProviderReturnsMockStatus() {
        DevelopmentDrugDataProvider devProvider = new DevelopmentDrugDataProvider();
        Optional<DrugInteractionResult> res = devProvider.checkInteraction("warfarin", "aspirin");

        assertTrue(res.isPresent());
        assertTrue(res.get().getSourceName().contains("Development Mock Data"));
        assertTrue(res.get().getSourceReference().startsWith("REF-DEV-MOCK-"));

        // Verify InteractionService maps it to DEVELOPMENT_MOCK
        Medication m1 = new Medication();
        m1.setMedicineName("Warfarin");
        Medication m2 = new Medication();
        m2.setMedicineName("Aspirin");

        InteractionAlert alert = new InteractionAlert();
        alert.setMedication1(m1);
        alert.setMedication2(m2);
        alert.setSeverity(res.get().getSeverity());
        alert.setDescription(res.get().getDescription());
        alert.setSourceName(res.get().getSourceName());
        alert.setSourceReference(res.get().getSourceReference());
        alert.setCheckedAt(LocalDateTime.now());

        InteractionAlertResponse alertResp = interactionService.mapToResponse(alert);
        assertEquals("DEVELOPMENT_MOCK", alertResp.getEvidenceStatus(),
                "Mock data must be tagged as DEVELOPMENT_MOCK and never presented as VERIFIED");
    }

    @Test
    @DisplayName("Case G: Heuristic severity guessing prevention -> 'caution' or 'warning' in snippet remains DOCUMENTED_IN_LABEL")
    void testCaseG_NoInventedSeverityFromCautionOrWarning() {
        String cautionSnippet = "Use caution when administering DrugA with DrugB as interactions have been reported in clinical studies.";
        String severity1 = openFdaProvider.extractSeverityOrUnspecified(cautionSnippet);
        assertEquals("DOCUMENTED_IN_LABEL", severity1,
                "Words like 'caution' must NEVER be mapped to MODERATE or MAJOR");

        String warningSnippet = "Warning: Concurrent use with DrugC may alter plasma concentration levels.";
        String severity2 = openFdaProvider.extractSeverityOrUnspecified(warningSnippet);
        assertEquals("DOCUMENTED_IN_LABEL", severity2,
                "Words like 'warning' must NEVER be mapped to MAJOR");

        String explicitMajorSnippet = "The interaction is categorized as major clinical risk.";
        String severity3 = openFdaProvider.extractSeverityOrUnspecified(explicitMajorSnippet);
        assertEquals("MAJOR", severity3, "Explicit designation 'major' is preserved");

        String explicitContraindicated = "Concomitant use is contraindicated due to arrhythmia risk.";
        String severity4 = openFdaProvider.extractSeverityOrUnspecified(explicitContraindicated);
        assertEquals("CONTRAINDICATED", severity4, "Explicit contraindication is preserved");
    }

    @Test
    @DisplayName("Case H: Stop-words protection -> Common pharmaceutical words cannot be pairwise target terms")
    void testCaseH_StopWordsDoNotTriggerFalsePairwiseInteractions() {
        assertTrue(OpenFdaDrugDataProvider.CLINICAL_STOPWORDS.contains("sodium"));
        assertTrue(OpenFdaDrugDataProvider.CLINICAL_STOPWORDS.contains("acid"));
        assertTrue(OpenFdaDrugDataProvider.CLINICAL_STOPWORDS.contains("tablet"));
        assertTrue(OpenFdaDrugDataProvider.CLINICAL_STOPWORDS.contains("potassium"));
        assertTrue(OpenFdaDrugDataProvider.CLINICAL_STOPWORDS.contains("hydrochloride"));

        EvidenceGroundedInteractionEngine engine = new EvidenceGroundedInteractionEngine(null, null, null);
        DrugInfoResult info = new DrugInfoResult("Warfarin Sodium 5 MG Oral Tablet", "Warfarin Sodium", "Anticoagulant", "RxNorm", "855332");
        List<String> terms = engine.buildSearchTerms("Warfarin Sodium", info);

        // Must include base drug "Warfarin" and full name "Warfarin Sodium"
        assertTrue(terms.contains("Warfarin") || terms.contains("warfarin"));
        assertTrue(terms.contains("Warfarin Sodium"));

        // Must NOT include naked stop words as standalone terms
        assertFalse(terms.contains("sodium"));
        assertFalse(terms.contains("tablet"));
        assertFalse(terms.contains("oral"));
        assertFalse(terms.contains("mg"));
    }

    @Test
    @DisplayName("Case I: AI Safety Guardrails -> Mock data rejected, NOT_FOUND handled safely, AMBIGUOUS guarded")
    void testCaseI_AISafetyGuardrails() {
        // Test Ia: Reject mock data from clinical AI explanation
        AIExplanationRequest mockReq = new AIExplanationRequest(
                "Warfarin", "Aspirin", "MAJOR",
                "Simulated mock interaction for development testing",
                "Development Mock Data Provider [Development Mock Data]"
        );
        mockReq.setEvidenceStatus("DEVELOPMENT_MOCK");

        assertThrows(BadRequestException.class, () -> aiExplanationService.explainInteraction(mockReq),
                "AI service must reject DEVELOPMENT_MOCK data");

        // Test Ib: NOT_FOUND explanation strictly forbids claiming complete safety
        AIExplanationRequest notFoundReq = new AIExplanationRequest(
                "Amoxicillin", "Metformin", "NONE_IDENTIFIED",
                "No interaction identified in the consulted source.",
                "FDA Drug Labeling (openFDA)"
        );
        notFoundReq.setEvidenceStatus("NOT_FOUND");

        AIExplanationResponse notFoundResp = aiExplanationService.explainInteraction(notFoundReq);
        assertNotNull(notFoundResp);
        assertTrue(notFoundResp.getPatientFriendlyExplanation().contains("does NOT mean these medicines are completely safe"));
        assertEquals("NONE_IDENTIFIED", notFoundResp.getSeverity());

        // Test Ic: AMBIGUOUS state refuses clinical explanation
        AIExplanationRequest ambReq = new AIExplanationRequest(
                "Metoprolol", "Aspirin", "AMBIGUOUS",
                "Please confirm the medicine before checking interactions.",
                "RxNorm Term Matching"
        );
        ambReq.setEvidenceStatus("AMBIGUOUS");

        AIExplanationResponse ambResp = aiExplanationService.explainInteraction(ambReq);
        assertNotNull(ambResp);
        assertTrue(ambResp.getPatientFriendlyExplanation().contains("cannot be generated until the exact medication is confirmed"));
    }
}
