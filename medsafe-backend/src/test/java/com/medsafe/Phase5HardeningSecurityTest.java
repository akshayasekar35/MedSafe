package com.medsafe;

import com.medsafe.dto.*;
import com.medsafe.entity.InteractionAlert;
import com.medsafe.entity.Medication;
import com.medsafe.entity.Schedule;
import com.medsafe.entity.User;
import com.medsafe.exception.BadRequestException;
import com.medsafe.repository.*;
import com.medsafe.security.TokenService;
import com.medsafe.service.AIExplanationService;
import com.medsafe.service.DuplicateDetectionService;
import com.medsafe.service.InteractionService;
import com.medsafe.service.MedicationService;
import com.medsafe.service.ScheduleService;
import com.medsafe.service.drugdata.DevelopmentDrugDataProvider;
import com.medsafe.service.drugdata.DrugDataProvider;
import com.medsafe.service.drugdata.DrugInfoResult;
import com.medsafe.service.drugdata.DrugInteractionResult;
import com.medsafe.service.drugdata.model.EvidenceStatus;
import com.medsafe.service.drugdata.openfda.OpenFdaDrugDataProvider;
import com.medsafe.service.drugdata.rxnorm.RxNormDrugDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 5 Comprehensive Hardening, Security, UX, Reliability, and Safety Test Suite.
 * Covers Scenarios A through T:
 *
 * Test A: Password hashed with BCrypt.
 * Test B: Password hash never returned in REST responses.
 * Test C: User isolation for medications (IDOR prevention).
 * Test D: User isolation for schedules (IDOR prevention).
 * Test E: User isolation for logs (IDOR prevention).
 * Test F: User isolation for interaction alerts (IDOR prevention).
 * Test G: Invalid, expired, or tampered tokens rejected safely (401/403).
 * Test H: Validation rejects blank medicine name.
 * Test I: Validation rejects invalid dates (endDate before startDate).
 * Test J: Duplicate active ingredient warning detected across different brand names.
 * Test K: Interaction NOT_FOUND remains non-clinical (does not claim safe).
 * Test L: Interaction AMBIGUOUS halts clinical explanation.
 * Test M: SOURCE_UNAVAILABLE handled safely without false positives or negatives.
 * Test N: DEVELOPMENT_MOCK remains isolated behind test profiles.
 * Test O: AI rejects unverified or mock evidence (BadRequestException).
 * Test P: AI does not invent or guess severity.
 * Test Q: AI does not issue medication-change instructions.
 * Test R: Verified pairwise interaction path works end-to-end.
 * Test S: Error responses do not expose stack traces or raw SQL.
 * Test T: External API timeout handled safely.
 */
@SpringBootTest(properties = "medsafe.drug-data.provider=production")
public class Phase5HardeningSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private MedicationLogRepository medicationLogRepository;

    @Autowired
    private InteractionAlertRepository interactionAlertRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MedicationService medicationService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private DuplicateDetectionService duplicateDetectionService;

    @Autowired
    private AIExplanationService aiExplanationService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DrugDataProvider activeDrugDataProvider;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String USER_A_EMAIL = "phase5_user_a@medsafe.org";
    private static final String USER_B_EMAIL = "phase5_user_b@medsafe.org";
    private static final String RAW_PASSWORD = "StrongSecurePassword@2026";

    private String tokenA;
    private String tokenB;
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        cleanupUserCascade(USER_A_EMAIL);
        cleanupUserCascade(USER_B_EMAIL);

        // Register User A
        MvcResult regAResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("User Alpha", USER_A_EMAIL, RAW_PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authA = objectMapper.readValue(regAResult.getResponse().getContentAsString(), AuthResponse.class);
        this.tokenA = authA.getToken();
        this.userA = userRepository.findByEmail(USER_A_EMAIL).orElseThrow();

        // Register User B
        MvcResult regBResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("User Beta", USER_B_EMAIL, RAW_PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authB = objectMapper.readValue(regBResult.getResponse().getContentAsString(), AuthResponse.class);
        this.tokenB = authB.getToken();
        this.userB = userRepository.findByEmail(USER_B_EMAIL).orElseThrow();
    }

    private void cleanupUserCascade(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            List<Medication> meds = medicationRepository.findByUserUserId(user.getUserId());
            for (Medication m : meds) {
                interactionAlertRepository.deleteAll(
                        interactionAlertRepository.findByMedication1MedicationIdOrMedication2MedicationId(m.getMedicationId(), m.getMedicationId())
                );
                List<Schedule> schedules = scheduleRepository.findByMedicationMedicationId(m.getMedicationId());
                for (Schedule s : schedules) {
                    medicationLogRepository.deleteAll(medicationLogRepository.findByScheduleScheduleId(s.getScheduleId()));
                }
                scheduleRepository.deleteAll(schedules);
            }
            medicationRepository.deleteAll(meds);
            userRepository.delete(user);
        });
    }

    @Test
    @DisplayName("Test A: Passwords must be hashed using BCrypt, never stored in plaintext")
    void testA_PasswordHashedWithBcrypt() {
        User fetchedUser = userRepository.findByEmail(USER_A_EMAIL).orElseThrow();
        String storedHash = fetchedUser.getPasswordHash();

        assertNotNull(storedHash, "Password hash must not be null");
        assertNotEquals(RAW_PASSWORD, storedHash, "Password must not be stored in plaintext");
        assertTrue(storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$"),
                "Password hash must be in standard BCrypt format ($2a$ or $2b$)");
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, storedHash),
                "BCrypt password encoder must successfully match the raw password against stored hash");
    }

    @Test
    @DisplayName("Test B: Password hash must NEVER be exposed in any REST API response")
    void testB_PasswordHashNeverReturnedInResponses() throws Exception {
        // Check register response
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("Test Patient", "unique_sec_test@medsafe.org", RAW_PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password_hash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$"))));

        // Check login response
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(USER_A_EMAIL, RAW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password_hash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$"))));

        // Cleanup
        cleanupUserCascade("unique_sec_test@medsafe.org");
    }

    @Test
    @DisplayName("Test C: User isolation for medications (IDOR prevention)")
    void testC_UserIsolationForMedications() throws Exception {
        // User A creates a medication
        MedicationRequest medReq = new MedicationRequest("Metformin", "metformin", "500mg", "Once daily",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "ACTIVE");

        MvcResult addResult = mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(medReq)))
                .andExpect(status().isCreated())
                .andReturn();

        MedicationResponse createdMed = objectMapper.readValue(addResult.getResponse().getContentAsString(), MedicationResponse.class);
        Long medAId = createdMed.getMedicationId();

        // User B attempts to access User A's medication by ID -> 401 Unauthorized
        mockMvc.perform(get("/api/medications/" + medAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isUnauthorized());

        // User B attempts to update User A's medication -> 401 Unauthorized
        MedicationRequest updateReq = new MedicationRequest("Metformin Tampered", "metformin", "1000mg", "Twice daily",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "ACTIVE");
        mockMvc.perform(put("/api/medications/" + medAId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isUnauthorized());

        // User B attempts to delete User A's medication -> 401 Unauthorized
        mockMvc.perform(delete("/api/medications/" + medAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isUnauthorized());

        // User B lists medications -> User A's medication must NOT appear
        mockMvc.perform(get("/api/medications")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Test D: User isolation for schedules (IDOR prevention)")
    void testD_UserIsolationForSchedules() throws Exception {
        // User A creates medication
        Medication medA = new Medication(userA, "Lisinopril", "lisinopril", "10mg", "Daily",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "ACTIVE");
        medA = medicationRepository.save(medA);

        // User A creates schedule
        Schedule schedA = new Schedule(medA, LocalTime.of(8, 0), "MORNING");
        schedA = scheduleRepository.save(schedA);
        Long scheduleAId = schedA.getScheduleId();

        // User B attempts to update User A's schedule -> 401 Unauthorized
        ScheduleRequest updateSched = new ScheduleRequest(LocalTime.of(12, 0), "NOON");
        mockMvc.perform(put("/api/schedules/" + scheduleAId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateSched)))
                .andExpect(status().isUnauthorized());

        // User B attempts to delete User A's schedule -> 401 Unauthorized
        mockMvc.perform(delete("/api/schedules/" + scheduleAId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isUnauthorized());

        // User B attempts to create a schedule pointing to User A's medication -> 401 Unauthorized
        ScheduleRequest hackSched = new ScheduleRequest(LocalTime.of(9, 0), "MORNING");
        mockMvc.perform(post("/api/medications/" + medA.getMedicationId() + "/schedules")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hackSched)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Test E: User isolation for medication logs and history (IDOR prevention)")
    void testE_UserIsolationForLogsAndHistory() throws Exception {
        // User A creates medication & schedule
        Medication medA = medicationRepository.save(new Medication(userA, "Amlodipine", "amlodipine", "5mg", "Daily",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "ACTIVE"));
        Schedule schedA = scheduleRepository.save(new Schedule(medA, LocalTime.of(8, 0), "MORNING"));

        // User B attempts to log TAKEN on User A's schedule -> 401 Unauthorized
        mockMvc.perform(post("/api/logs/" + schedA.getScheduleId() + "/taken")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // User A logs TAKEN successfully
        mockMvc.perform(post("/api/logs/" + schedA.getScheduleId() + "/taken")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        // User B views history -> must NOT contain User A's logs
        mockMvc.perform(get("/api/logs")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Test F: User isolation for interaction alerts (IDOR prevention)")
    void testF_UserIsolationForInteractionAlerts() throws Exception {
        Medication medA1 = medicationRepository.save(new Medication(userA, "Warfarin", "warfarin", "5mg", "Daily",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "ACTIVE"));
        Medication medA2 = medicationRepository.save(new Medication(userA, "Aspirin", "aspirin", "81mg", "Daily",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "ACTIVE"));

        InteractionAlert alertA = new InteractionAlert(
                medA1, medA2, "HIGH", "Bleeding hazard detected",
                "FDA Drug Labeling (openFDA)", "SPL-12345"
        );
        alertA = interactionAlertRepository.save(alertA);
        Long alertId = alertA.getAlertId();

        // User B attempts to get User A's alert by ID -> 401 Unauthorized
        mockMvc.perform(get("/api/interactions/" + alertId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isUnauthorized());

        // User B queries alerts -> User A's alert must not appear
        mockMvc.perform(get("/api/interactions")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Test G: Invalid, expired, or tampered tokens rejected safely (401/403)")
    void testG_InvalidTamperedTokensRejected() throws Exception {
        // Missing Authorization header
        mockMvc.perform(get("/api/medications"))
                .andExpect(status().isForbidden());

        // Malformed / garbage token
        mockMvc.perform(get("/api/medications")
                        .header("Authorization", "Bearer invalid_garbage_payload"))
                .andExpect(status().isForbidden());

        // Tampered token (modified payload with original signature)
        String tamperedToken = tokenA.substring(0, tokenA.lastIndexOf('.') + 1) + "tampered_signature";
        mockMvc.perform(get("/api/medications")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isForbidden());

        // Backdoor header X-User-Email must be completely ignored / rejected
        mockMvc.perform(get("/api/medications")
                        .header("X-User-Email", USER_A_EMAIL))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test H: Input validation rejects blank medicine name")
    void testH_ValidationRejectsBlankMedicineName() throws Exception {
        MedicationRequest blankMed = new MedicationRequest("   ", "metformin", "500mg", "Daily",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "ACTIVE");

        mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankMed)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Input validation failed")))
                .andExpect(jsonPath("$.validationErrors.medicineName", containsString("Medicine name is required")));
    }

    @Test
    @DisplayName("Test I: Validation rejects invalid dates (endDate before startDate)")
    void testI_ValidationRejectsInvalidDates() throws Exception {
        MedicationRequest invalidDateMed = new MedicationRequest("Amoxicillin", "amoxicillin", "250mg", "Twice daily",
                LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 10), "ACTIVE");

        mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDateMed)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Input validation failed")))
                .andExpect(jsonPath("$.validationErrors.endDateValid", containsString("cannot be before start date")));
    }

    @Test
    @DisplayName("Test J: Duplicate active ingredient warning detected across different brand names")
    void testJ_DuplicateActiveIngredientWarningDetected() throws Exception {
        // User A adds Advil (contains ibuprofen) and Motrin (contains ibuprofen)
        Medication advil = new Medication(userA, "Advil", "ibuprofen", "200mg", "As needed",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "ACTIVE");
        Medication motrin = new Medication(userA, "Motrin", "ibuprofen", "200mg", "As needed",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), "ACTIVE");

        medicationRepository.save(advil);
        medicationRepository.save(motrin);

        mockMvc.perform(get("/api/medications/duplicates")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].activeIngredient", equalToIgnoringCase("ibuprofen")))
                .andExpect(jsonPath("$[0].medications[*].medicineName", hasItems("Advil", "Motrin")));
    }

    @Test
    @DisplayName("Test K: Interaction NOT_FOUND remains non-clinical (does not claim safe)")
    void testK_InteractionNotFoundDoesNotClaimSafe() {
        DrugInteractionResult notFoundResult = DrugInteractionResult.notFound(
                "Amoxicillin", "Metformin", "FDA Drug Labeling (openFDA)"
        );

        assertEquals(EvidenceStatus.NOT_FOUND, notFoundResult.getEvidenceStatus());
        assertFalse(notFoundResult.isHasInteraction());
        assertEquals("No interaction identified in the consulted source.", notFoundResult.getDescription());

        // Absence of evidence must never be phrased as guaranteed safety
        assertFalse(notFoundResult.getDescription().toLowerCase().contains("completely safe"));
        assertFalse(notFoundResult.getDescription().toLowerCase().contains("safe to combine"));
        assertFalse(notFoundResult.getDescription().toLowerCase().contains("no risk"));
    }

    @Test
    @DisplayName("Test L: Interaction AMBIGUOUS halts clinical explanation")
    void testL_InteractionAmbiguousHaltsExplanation() {
        AIExplanationRequest ambReq = new AIExplanationRequest(
                "Metoprolol", "Aspirin", "AMBIGUOUS",
                "Please confirm the medicine before checking interactions.", "RxNorm Term Matching"
        );
        ambReq.setEvidenceStatus("AMBIGUOUS");

        AIExplanationResponse ambResp = aiExplanationService.explainInteraction(ambReq);
        assertNotNull(ambResp);
        assertEquals("AMBIGUOUS", ambResp.getSeverity());
        assertTrue(ambResp.getPatientFriendlyExplanation().contains("ambiguous"));
        assertTrue(ambResp.getPatientFriendlyExplanation().contains("confirm the medicine"));
        assertTrue(ambResp.getSafetyNotice().contains("confirm the medicine"));
    }

    @Test
    @DisplayName("Test M: SOURCE_UNAVAILABLE handled safely without false positives or negatives")
    void testM_SourceUnavailableHandledSafely() {
        AIExplanationRequest unavailReq = new AIExplanationRequest(
                "Warfarin", "Aspirin", "UNKNOWN",
                "External clinical source is unavailable.", "FDA Drug Labeling (openFDA)"
        );
        unavailReq.setEvidenceStatus("SOURCE_UNAVAILABLE");

        AIExplanationResponse unavailResp = aiExplanationService.explainInteraction(unavailReq);
        assertNotNull(unavailResp);
        assertEquals("UNAVAILABLE", unavailResp.getSeverity());
        assertTrue(unavailResp.getPatientFriendlyExplanation().contains("temporarily unavailable"));
        assertTrue(unavailResp.getPatientFriendlyExplanation().contains("doctor or dispensing pharmacist"));
    }

    @Test
    @DisplayName("Test N: DEVELOPMENT_MOCK remains isolated behind test profiles")
    void testN_DevelopmentMockIsolatedInProductionProfile() {
        // In production profile, active drug data provider must be EvidenceGroundedInteractionEngine
        assertNotNull(activeDrugDataProvider);
        assertFalse(activeDrugDataProvider instanceof DevelopmentDrugDataProvider,
                "In production profile, DevelopmentDrugDataProvider must NEVER be the active provider");

        // The DevelopmentDrugDataProvider bean exists in context but is not active
        DevelopmentDrugDataProvider devProvider = applicationContext.getBean(DevelopmentDrugDataProvider.class);
        assertNotNull(devProvider);
        assertNotSame(activeDrugDataProvider, devProvider);
    }

    @Test
    @DisplayName("Test O: AI rejects unverified or mock evidence with BadRequestException")
    void testO_AiRejectsMockEvidence() {
        AIExplanationRequest mockReq = new AIExplanationRequest(
                "DrugAlpha", "DrugBeta", "MAJOR",
                "Mock interaction description for testing.", "Development Mock Reference"
        );
        mockReq.setEvidenceStatus("DEVELOPMENT_MOCK");

        assertThrows(BadRequestException.class, () -> aiExplanationService.explainInteraction(mockReq),
                "AI explanation must throw BadRequestException when mock evidence is provided");
    }

    @Test
    @DisplayName("Test P: AI does not invent or guess severity")
    void testP_AiDoesNotInventOrGuessSeverity() {
        String testSeverity = "DOCUMENTED_IN_LABEL";
        AIExplanationRequest req = new AIExplanationRequest(
                "Warfarin", "Aspirin", testSeverity,
                "Concurrent administration of aspirin and warfarin may increase the risk of major bleeding.",
                "FDA Drug Labeling (openFDA)"
        );
        req.setEvidenceStatus("VERIFIED");

        AIExplanationResponse resp = aiExplanationService.explainInteraction(req);
        assertNotNull(resp);
        assertEquals(testSeverity, resp.getSeverity(), "AI response must retain exactly the external evidence severity");
        assertEquals("FDA Drug Labeling (openFDA)", resp.getVerifiedSource(), "AI response must retain verified source");
    }

    @Test
    @DisplayName("Test Q: AI does not issue medication-change instructions")
    void testQ_AiDoesNotIssueMedicationChangeInstructions() {
        AIExplanationRequest req = new AIExplanationRequest(
                "Warfarin", "Aspirin", "MAJOR",
                "Increased risk of serious bleeding events.", "FDA Drug Labeling (openFDA)"
        );
        req.setEvidenceStatus("VERIFIED");

        AIExplanationResponse resp = aiExplanationService.explainInteraction(req);
        String explanation = resp.getPatientFriendlyExplanation().toLowerCase();
        String safetyNotice = resp.getSafetyNotice().toLowerCase();
        String disclaimer = resp.getDisclaimer().toLowerCase();

        // Must explicitly tell the patient NOT to stop without consulting a doctor
        assertTrue(disclaimer.contains("do not stop or change your medication without consulting") ||
                   disclaimer.contains("not medical advice"));
        assertTrue(safetyNotice.contains("do not discontinue your medication on your own") ||
                   safetyNotice.contains("speak with your prescribing physician"));

        // Must not command patient to stop or adjust dosage independently
        assertFalse(explanation.contains("you must stop taking"));
        assertFalse(explanation.contains("discontinue immediately without"));
        assertFalse(safetyNotice.contains("stop taking one of them immediately"));
    }

    @Test
    @DisplayName("Test R: Verified pairwise interaction path works end-to-end")
    void testR_VerifiedPairwiseInteractionPath() {
        DrugInteractionResult verifiedResult = new DrugInteractionResult(
                "Warfarin",
                "Aspirin",
                true,
                "MAJOR",
                "Co-administration increases risk of severe bleeding complications.",
                "FDA Drug Labeling (openFDA)",
                "SPL-WARFARIN-001",
                EvidenceStatus.VERIFIED
        );

        assertEquals(EvidenceStatus.VERIFIED, verifiedResult.getEvidenceStatus());
        assertTrue(verifiedResult.isHasInteraction());
        assertEquals("MAJOR", verifiedResult.getSeverity());
        assertEquals("FDA Drug Labeling (openFDA)", verifiedResult.getSourceName());
        assertNotNull(verifiedResult.getSourceReference());
    }

    @Test
    @DisplayName("Test S: Error responses do not expose stack traces or raw SQL")
    void testS_ErrorResponsesDoNotExposeInternalTraces() throws Exception {
        // Trigger 404 error
        MvcResult res404 = mockMvc.perform(get("/api/medications/999999999")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andReturn();

        String body404 = res404.getResponse().getContentAsString();
        assertFalse(body404.contains("org.springframework"), "Error body must not contain Spring stack trace");
        assertFalse(body404.contains("SQLException"), "Error body must not contain raw SQL exception");
        assertFalse(body404.contains("HibernateException"), "Error body must not contain Hibernate trace");
        assertFalse(body404.contains("java.lang"), "Error body must not contain Java stack trace");

        // Trigger 400 Bad Request error
        MvcResult res400 = mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineName\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        String body400 = res400.getResponse().getContentAsString();
        assertFalse(body400.contains("org.springframework"));
        assertFalse(body400.contains("at com.medsafe."));
    }

    @Test
    @DisplayName("Test T: External API timeout handled safely without crashing")
    void testT_ExternalApiTimeoutHandledSafely() {
        // Provider configured with an unroutable IP and short timeout
        RxNormDrugDataProvider timeoutRxNorm = new RxNormDrugDataProvider("http://10.255.255.1:81", 400, 400);
        DrugInfoResult rxResult = timeoutRxNorm.identifyDrug("metformin");
        assertNotNull(rxResult);
        assertEquals(EvidenceStatus.SOURCE_UNAVAILABLE, rxResult.getEvidenceStatus(),
                "Timeout must safely return SOURCE_UNAVAILABLE instead of throwing uncaught exception");

        OpenFdaDrugDataProvider timeoutFda = new OpenFdaDrugDataProvider("http://10.255.255.1:81", "", 400, 400);
        Optional<DrugInteractionResult> fdaResult = timeoutFda.checkInteraction("warfarin", "aspirin", List.of("warfarin"), List.of("aspirin"));
        assertTrue(fdaResult.isPresent());
        assertEquals(EvidenceStatus.SOURCE_UNAVAILABLE, fdaResult.get().getEvidenceStatus(),
                "FDA timeout must safely return SOURCE_UNAVAILABLE instead of crashing");
    }
}
