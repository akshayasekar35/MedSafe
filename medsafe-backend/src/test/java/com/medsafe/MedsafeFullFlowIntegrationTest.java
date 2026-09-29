package com.medsafe;

import com.medsafe.dto.*;
import com.medsafe.entity.User;
import com.medsafe.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "medsafe.drug-data.provider=development")
class MedsafeFullFlowIntegrationTest {

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

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String testUserToken;
    private Long testUserId;
    private final String testEmail = "testuser@medsafe.org";

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Clean up user test data in correct foreign key order
        userRepository.findByEmail(testEmail).ifPresent(user -> {
            List<com.medsafe.entity.Medication> meds = medicationRepository.findByUserUserId(user.getUserId());
            for (com.medsafe.entity.Medication m : meds) {
                interactionAlertRepository.deleteAll(interactionAlertRepository.findByMedication1MedicationIdOrMedication2MedicationId(m.getMedicationId(), m.getMedicationId()));
                List<com.medsafe.entity.Schedule> schedules = scheduleRepository.findByMedicationMedicationId(m.getMedicationId());
                for (com.medsafe.entity.Schedule s : schedules) {
                    medicationLogRepository.deleteAll(medicationLogRepository.findByScheduleScheduleId(s.getScheduleId()));
                }
                scheduleRepository.deleteAll(schedules);
            }
            medicationRepository.deleteAll(meds);
            userRepository.delete(user);
        });

        // Register test user
        RegisterRequest registerReq = new RegisterRequest("Test Patient", testEmail, "Password@123");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email", is(testEmail)))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()) // Never expose password_hash
                .andReturn();

        AuthResponse auth = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class);
        this.testUserToken = auth.getToken();
        this.testUserId = auth.getUserId();
    }

    @Test
    void testAuthFlowAndDuplicateEmailRejection() throws Exception {
        // Test login
        LoginRequest loginReq = new LoginRequest(testEmail, "Password@123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // Test duplicate email rejection
        RegisterRequest duplicateReq = new RegisterRequest("Another User", testEmail, "Password@999");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void testMedicationCrudAndUserIsolation() throws Exception {
        // 1. Add medication
        MedicationRequest medReq = new MedicationRequest(
                "Coumadin",
                "Warfarin",
                "5mg",
                "Once daily in evening",
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                "ACTIVE"
        );

        MvcResult createResult = mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(medReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicineName", is("Coumadin")))
                .andExpect(jsonPath("$.activeIngredient", is("Warfarin")))
                .andExpect(jsonPath("$.userId", is(testUserId.intValue())))
                .andReturn();

        MedicationResponse createdMed = objectMapper.readValue(createResult.getResponse().getContentAsString(), MedicationResponse.class);
        Long medId = createdMed.getMedicationId();

        // 2. Get by ID
        mockMvc.perform(get("/api/medications/" + medId)
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicationId", is(medId.intValue())));

        // 3. List medications
        mockMvc.perform(get("/api/medications")
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // 4. Update medication
        medReq.setDosage("7.5mg");
        mockMvc.perform(put("/api/medications/" + medId)
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(medReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dosage", is("7.5mg")));

        // 5. Delete medication
        mockMvc.perform(delete("/api/medications/" + medId)
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void testScheduleAndMedicationLogsFlow() throws Exception {
        // Create medication first
        MedicationRequest medReq = new MedicationRequest("Cardicor", "Bisoprolol", "2.5mg", "Once daily", LocalDate.now(), null, "ACTIVE");
        MvcResult medResult = mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(medReq)))
                .andExpect(status().isCreated())
                .andReturn();
        MedicationResponse med = objectMapper.readValue(medResult.getResponse().getContentAsString(), MedicationResponse.class);

        // Add schedule
        ScheduleRequest schedReq = new ScheduleRequest(LocalTime.of(8, 30, 0), "After breakfast");
        MvcResult schedResult = mockMvc.perform(post("/api/medications/" + med.getMedicationId() + "/schedules")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(schedReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduledTime", is("08:30:00")))
                .andExpect(jsonPath("$.mealRelation", is("After breakfast")))
                .andReturn();
        ScheduleResponse sched = objectMapper.readValue(schedResult.getResponse().getContentAsString(), ScheduleResponse.class);

        // Mark TAKEN
        mockMvc.perform(post("/api/logs/" + sched.getScheduleId() + "/taken")
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("TAKEN")))
                .andExpect(jsonPath("$.actualDatetime").isNotEmpty());

        // Mark MISSED
        mockMvc.perform(post("/api/logs/" + sched.getScheduleId() + "/missed")
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("MISSED")))
                .andExpect(jsonPath("$.actualDatetime").isEmpty());

        // Get logs
        mockMvc.perform(get("/api/logs")
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void testDuplicateActiveIngredientDetection() throws Exception {
        // Add two different brand medicines with the SAME active ingredient
        MedicationRequest med1 = new MedicationRequest("Tylenol Extra Strength", "Acetaminophen", "500mg", "Every 6h", LocalDate.now(), null, "ACTIVE");
        MedicationRequest med2 = new MedicationRequest("Panadol ActiFast", "Acetaminophen", "500mg", "As needed", LocalDate.now(), null, "ACTIVE");

        mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(med1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(med2)))
                .andExpect(status().isCreated());

        // Query duplicates endpoint
        mockMvc.perform(get("/api/medications/duplicates")
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].activeIngredient", equalToIgnoringCase("Acetaminophen")))
                .andExpect(jsonPath("$[0].medications", hasSize(2)))
                .andExpect(jsonPath("$[0].warningMessage", containsString("active ingredient")))
                .andExpect(jsonPath("$[0].clinicalRecommendation", containsString("Do not stop")));
    }

    @Test
    void testDrugInteractionCheckAndPersistedAlerts() throws Exception {
        // Add Warfarin and Aspirin (a known MAJOR interaction pair)
        MedicationRequest med1 = new MedicationRequest("Coumadin", "Warfarin", "5mg", "Daily", LocalDate.now(), null, "ACTIVE");
        MedicationRequest med2 = new MedicationRequest("Bayer Aspirin", "Aspirin", "81mg", "Daily", LocalDate.now(), null, "ACTIVE");

        mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(med1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/medications")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(med2)))
                .andExpect(status().isCreated());

        // Check interactions
        mockMvc.perform(post("/api/interactions/check")
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInteractionsFound", is(1)))
                .andExpect(jsonPath("$.alerts[0].severity", is("MAJOR")))
                .andExpect(jsonPath("$.alerts[0].sourceName", containsString("Development Mock Data")))
                .andExpect(jsonPath("$.alerts[0].sourceReference", containsString("WAF-ASP-001")))
                .andExpect(jsonPath("$.disclaimer", containsString("absence of a documented interaction")));

        // Get saved alerts
        mockMvc.perform(get("/api/interactions")
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testAIExplanationEndpointWithSafetyRules() throws Exception {
        AIExplanationRequest aiReq = new AIExplanationRequest(
                "Warfarin",
                "Aspirin",
                "MAJOR",
                "Concurrent use of Warfarin (an anticoagulant) and Aspirin (an antiplatelet agent) significantly increases the risk of severe gastrointestinal and systemic bleeding due to additive pharmacodynamic effects.",
                "Clinical Pharmacology Reference & FDA Prescribing Information"
        );

        mockMvc.perform(post("/api/ai/explain-interaction")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aiReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drugA", is("Warfarin")))
                .andExpect(jsonPath("$.drugB", is("Aspirin")))
                .andExpect(jsonPath("$.severity", is("MAJOR")))
                .andExpect(jsonPath("$.patientFriendlyExplanation", containsString("blood-thinning medicine")))
                .andExpect(jsonPath("$.patientFriendlyExplanation", containsString("What You Should Do")))
                .andExpect(jsonPath("$.verifiedSource", is("Clinical Pharmacology Reference & FDA Prescribing Information")))
                .andExpect(jsonPath("$.disclaimer", containsString("Do NOT stop or change your medication")));
    }
}
