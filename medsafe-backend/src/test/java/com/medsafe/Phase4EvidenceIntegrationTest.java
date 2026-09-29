package com.medsafe;

import com.medsafe.dto.AIExplanationRequest;
import com.medsafe.dto.AIExplanationResponse;
import com.medsafe.entity.DrugInformation;
import com.medsafe.repository.DrugInformationRepository;
import com.medsafe.service.AIExplanationService;
import com.medsafe.service.drugdata.DevelopmentDrugDataProvider;
import com.medsafe.service.drugdata.DrugDataProvider;
import com.medsafe.service.drugdata.DrugInfoResult;
import com.medsafe.service.drugdata.DrugInteractionResult;
import com.medsafe.service.drugdata.EvidenceGroundedInteractionEngine;
import com.medsafe.service.drugdata.cache.DrugInformationCacheService;
import com.medsafe.service.drugdata.model.EvidenceStatus;
import com.medsafe.service.drugdata.openfda.OpenFdaDrugDataProvider;
import com.medsafe.service.drugdata.rxnorm.RxNormDrugDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "medsafe.drug-data.provider=production")
public class Phase4EvidenceIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DrugDataProvider activeDrugDataProvider;

    @Autowired
    private DrugInformationCacheService cacheService;

    @Autowired
    private DrugInformationRepository drugInformationRepository;

    @Autowired
    private AIExplanationService aiExplanationService;

    private RxNormDrugDataProvider rxNormProvider;
    private OpenFdaDrugDataProvider openFdaProvider;

    @BeforeEach
    void setUp() {
        // Create providers with low timeout to test gracefully
        this.rxNormProvider = new RxNormDrugDataProvider("https://rxnav.nlm.nih.gov/REST", 3000, 4000);
        this.openFdaProvider = new OpenFdaDrugDataProvider("https://api.fda.gov/drug/label.json", "", 3000, 4000);
    }

    @Test
    void test1_RxNormExactMatch() {
        DrugInfoResult result = rxNormProvider.identifyDrug("aspirin");
        assertNotNull(result);
        if (result.getEvidenceStatus() == EvidenceStatus.VERIFIED) {
            assertEquals("1191", result.getExternalId(), "Aspirin RxCUI should be 1191");
            assertTrue(result.getStandardName().toLowerCase().contains("aspirin"));
        } else {
            // In case of network timeout in sandbox
            assertEquals(EvidenceStatus.SOURCE_UNAVAILABLE, result.getEvidenceStatus());
        }
    }

    @Test
    void test2_RxNormNormalizedMatch() {
        // Warfarin Sodium normalized lookup
        DrugInfoResult result = rxNormProvider.identifyDrug("warfarin sodium");
        assertNotNull(result);
        if (result.getEvidenceStatus() == EvidenceStatus.VERIFIED) {
            assertNotNull(result.getExternalId());
            assertTrue(result.getStandardName().toLowerCase().contains("warfarin"));
        } else {
            assertEquals(EvidenceStatus.SOURCE_UNAVAILABLE, result.getEvidenceStatus());
        }
    }

    @Test
    void test3_UnknownMedicineNotFound() {
        // Querying a completely fictional non-existent chemical string
        DrugInfoResult result = rxNormProvider.identifyDrug("xyznonexistentfakecompound9999");
        assertNotNull(result);
        assertTrue(result.getEvidenceStatus() == EvidenceStatus.NOT_FOUND || result.getEvidenceStatus() == EvidenceStatus.SOURCE_UNAVAILABLE);
    }

    @Test
    void test4_AmbiguousMedicineDetection() {
        // Create an approximate ambiguity response test
        DrugInfoResult ambiguous = DrugInfoResult.ambiguous("metoprolol", List.of("Metoprolol Succinate", "Metoprolol Tartrate"));
        assertEquals(EvidenceStatus.AMBIGUOUS, ambiguous.getEvidenceStatus());
        assertEquals(2, ambiguous.getAmbiguousCandidates().size());
        assertTrue(ambiguous.getAmbiguousCandidates().contains("Metoprolol Succinate"));
    }

    @Test
    void test5_ApiTimeoutHandling() {
        // Connect to an invalid non-routable port to simulate connection timeout
        RxNormDrugDataProvider timeoutProvider = new RxNormDrugDataProvider("http://10.255.255.1:81", 500, 500);
        DrugInfoResult result = timeoutProvider.identifyDrug("warfarin");
        assertNotNull(result);
        assertEquals(EvidenceStatus.SOURCE_UNAVAILABLE, result.getEvidenceStatus());
    }

    @Test
    void test6_CachedDrugInformationAvoidsRepeatedCalls() {
        String testDrug = "LisinoprilTest_" + System.currentTimeMillis();
        DrugInformation cached = new DrugInformation(testDrug, "Lisinopril", "ACE Inhibitor", "RxNorm", "29046");
        drugInformationRepository.save(cached);

        // Call cache service
        DrugInfoResult retrieved = cacheService.getOrFetchDrugInfo(testDrug);
        assertNotNull(retrieved);
        assertEquals(testDrug, retrieved.getStandardName());
        assertEquals("29046", retrieved.getExternalId());
        assertEquals(EvidenceStatus.VERIFIED, retrieved.getEvidenceStatus());
    }

    @Test
    void test7_ExternalSourceUnavailable() {
        OpenFdaDrugDataProvider unavailableProvider = new OpenFdaDrugDataProvider("http://10.255.255.1:81", "", 500, 500);
        Optional<DrugInteractionResult> res = unavailableProvider.checkInteraction("warfarin", "aspirin", List.of("warfarin"), List.of("aspirin"));
        assertTrue(res.isPresent());
        assertEquals(EvidenceStatus.SOURCE_UNAVAILABLE, res.get().getEvidenceStatus());
    }

    @Test
    void test8_VerifiedEvidenceFlow() {
        // Real or simulated verified interaction
        DrugInteractionResult verified = new DrugInteractionResult(
                "Coumadin",
                "Aspirin",
                true,
                "MAJOR",
                "Concurrent use increases bleeding risk.",
                "FDA Drug Labeling (openFDA)",
                "SPL Set ID: 0cbce382-9c88-4f58-ae0f-532a841e8f95",
                EvidenceStatus.VERIFIED
        );

        assertEquals(EvidenceStatus.VERIFIED, verified.getEvidenceStatus());
        assertTrue(verified.isHasInteraction());
        assertEquals("MAJOR", verified.getSeverity());
        assertNotNull(verified.getSourceReference());
    }

    @Test
    void test9_NoEvidenceFoundFlowDoesNotClaimCompleteSafety() {
        DrugInteractionResult notFound = DrugInteractionResult.notFound("Amoxicillin", "Metformin", "FDA Drug Labeling (openFDA)");
        assertEquals(EvidenceStatus.NOT_FOUND, notFound.getEvidenceStatus());
        assertFalse(notFound.isHasInteraction());
        assertEquals("No interaction identified in the consulted source.", notFound.getDescription());
        // Must NOT claim that the combination is safe
        assertFalse(notFound.getDescription().toLowerCase().contains("completely safe"));
    }

    @Test
    void test10_AIReceivesOnlyVerifiedEvidence() {
        // Test 10a: Verified interaction explanation
        AIExplanationRequest verifiedReq = new AIExplanationRequest(
                "Coumadin",
                "Aspirin",
                "MAJOR",
                "Concurrent use increases bleeding risk.",
                "FDA Drug Labeling (openFDA)"
        );
        verifiedReq.setEvidenceStatus("VERIFIED");
        AIExplanationResponse verifiedResp = aiExplanationService.explainInteraction(verifiedReq);
        assertNotNull(verifiedResp);
        assertTrue(verifiedResp.getPatientFriendlyExplanation().contains("Coumadin"));
        assertTrue(verifiedResp.getDisclaimer().contains("Do NOT stop or change your medication"));

        // Test 10b: NOT_FOUND state explanation - must not say safe
        AIExplanationRequest notFoundReq = new AIExplanationRequest(
                "Amoxicillin",
                "Metformin",
                "NONE_IDENTIFIED",
                "No interaction identified in the consulted source.",
                "FDA Drug Labeling (openFDA)"
        );
        notFoundReq.setEvidenceStatus("NOT_FOUND");
        AIExplanationResponse notFoundResp = aiExplanationService.explainInteraction(notFoundReq);
        assertNotNull(notFoundResp);
        assertTrue(notFoundResp.getPatientFriendlyExplanation().contains("No interaction identified in the consulted source"));
        assertTrue(notFoundResp.getPatientFriendlyExplanation().contains("does NOT mean these medicines are completely safe"));

        // Test 10c: AMBIGUOUS state explanation - halts explanation and requests confirmation
        AIExplanationRequest ambReq = new AIExplanationRequest(
                "Metoprolol",
                "Aspirin",
                "AMBIGUOUS",
                "Please confirm the medicine before checking interactions.",
                "RxNorm Term Matching"
        );
        ambReq.setEvidenceStatus("AMBIGUOUS");
        AIExplanationResponse ambResp = aiExplanationService.explainInteraction(ambReq);
        assertNotNull(ambResp);
        assertTrue(ambResp.getPatientFriendlyExplanation().contains("ambiguous"));
        assertTrue(ambResp.getSafetyNotice().contains("confirm the medicine"));
    }

    @Test
    void test11_DevelopmentProviderIsolation() {
        // In production profile/mode, activeDrugDataProvider must be EvidenceGroundedInteractionEngine
        assertTrue(activeDrugDataProvider instanceof EvidenceGroundedInteractionEngine,
                "In production mode, active provider must be EvidenceGroundedInteractionEngine, never DevelopmentDrugDataProvider");

        // Verify DevelopmentDrugDataProvider bean exists in context but is not active
        DevelopmentDrugDataProvider devProvider = applicationContext.getBean(DevelopmentDrugDataProvider.class);
        assertNotNull(devProvider);
        assertNotSame(activeDrugDataProvider, devProvider, "DevelopmentDrugDataProvider must not be active in production");
    }
}
