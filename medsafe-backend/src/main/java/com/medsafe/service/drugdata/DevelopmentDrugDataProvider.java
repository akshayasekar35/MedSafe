package com.medsafe.service.drugdata;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * DEVELOPMENT / TESTING MOCK IMPLEMENTATION
 * 
 * IMPORTANT: This class provides structured reference interactions for local development
 * and testing of the medication safety flow. All entries are clearly labeled as Mock Data
 * from Development Reference Datasets. Real production deployments should configure an
 * official external provider such as openFDA or RxNorm.
 */
@Component
public class DevelopmentDrugDataProvider implements DrugDataProvider {

    public static final String PROVIDER_NAME = "Development Mock Data Provider (Simulated Reference Knowledgebase)";
    public static final String SOURCE_REF_PREFIX = "REF-DEV-MOCK-";

    private final List<InteractionRecord> referenceDatabase = new ArrayList<>();

    public DevelopmentDrugDataProvider() {
        initReferenceDatabase();
    }

    private void initReferenceDatabase() {
        // Clinical reference pairs for development testing
        addRecord("warfarin", "aspirin", "MAJOR",
                "Concurrent use of Warfarin (an anticoagulant) and Aspirin (an antiplatelet agent) significantly increases the risk of severe gastrointestinal and systemic bleeding due to additive pharmacodynamic effects.",
                "Clinical Pharmacology Reference & FDA Prescribing Information",
                SOURCE_REF_PREFIX + "WAF-ASP-001");

        addRecord("lisinopril", "spironolactone", "MODERATE",
                "Combining Lisinopril (an ACE inhibitor) with Spironolactone (a potassium-sparing diuretic) can result in clinically significant hyperkalemia (high blood potassium), potentially triggering cardiac arrhythmias.",
                "FDA Drug Safety Database & RxNorm Reference",
                SOURCE_REF_PREFIX + "LIS-SPI-002");

        addRecord("ibuprofen", "lisinopril", "MODERATE",
                "Nonsteroidal anti-inflammatory drugs (NSAIDs) such as Ibuprofen may diminish the antihypertensive effect of ACE inhibitors like Lisinopril and increase the risk of renal impairment.",
                "NIH DailyMed Database",
                SOURCE_REF_PREFIX + "IBU-LIS-003");

        addRecord("simvastatin", "clarithromycin", "MAJOR",
                "Clarithromycin strongly inhibits CYP3A4 metabolism of Simvastatin, leading to markedly elevated statin plasma levels and increased risk of myopathy and rhabdomyolysis.",
                "FDA Clinical Drug Interaction Guidelines",
                SOURCE_REF_PREFIX + "SIM-CLA-004");

        addRecord("metformin", "contrast media", "SEVERE",
                "Iodinated contrast agents in patients taking Metformin may induce acute renal impairment and lactic acidosis. Metformin should temporarily be withheld prior to contrast procedures.",
                "American College of Radiology (ACR) & FDA Warning",
                SOURCE_REF_PREFIX + "MET-CON-005");

        addRecord("ciprofloxacin", "ibuprofen", "MODERATE",
                "Co-administration of fluoroquinolones (Ciprofloxacin) with NSAIDs (Ibuprofen) may increase the risk of central nervous system stimulation and convulsive seizures.",
                "National Library of Medicine Clinical Review",
                SOURCE_REF_PREFIX + "CIP-IBU-006");

        addRecord("methotrexate", "ibuprofen", "SEVERE",
                "NSAIDs like Ibuprofen can reduce the renal tubular clearance of Methotrexate, leading to elevated and potentially fatal Methotrexate hematologic and gastrointestinal toxicity.",
                "FDA Black Box Warning & Oncology Guidance",
                SOURCE_REF_PREFIX + "MTX-IBU-007");

        addRecord("paracetamol", "warfarin", "MINOR",
                "Chronic regular use of high-dose Paracetamol (Acetaminophen) may enhance the anticoagulant effect of Warfarin, requiring closer INR monitoring.",
                "British National Formulary (BNF) Reference",
                SOURCE_REF_PREFIX + "PCM-WAF-008");
    }

    private void addRecord(String drugA, String drugB, String severity, String description, String sourceName, String sourceRef) {
        referenceDatabase.add(new InteractionRecord(drugA.toLowerCase(), drugB.toLowerCase(), severity, description, sourceName, sourceRef));
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public DrugInfoResult identifyDrug(String medicineName) {
        if (medicineName == null || medicineName.trim().isEmpty()) {
            return null;
        }
        String clean = medicineName.trim();
        return new DrugInfoResult(
                clean,
                clean,
                "General Therapeutic Class (Development Mock)",
                PROVIDER_NAME,
                "MOCK-" + Math.abs(clean.toLowerCase().hashCode())
        );
    }

    @Override
    public Optional<DrugInteractionResult> checkInteraction(String drugA, String drugB) {
        if (drugA == null || drugB == null) {
            return Optional.empty();
        }

        String normA = normalizeName(drugA);
        String normB = normalizeName(drugB);

        for (InteractionRecord record : referenceDatabase) {
            boolean matchDirect = (normA.contains(record.drugA) || record.drugA.contains(normA))
                    && (normB.contains(record.drugB) || record.drugB.contains(normB));

            boolean matchReverse = (normA.contains(record.drugB) || record.drugB.contains(normA))
                    && (normB.contains(record.drugA) || record.drugA.contains(normB));

            if (matchDirect || matchReverse) {
                return Optional.of(new DrugInteractionResult(
                        drugA,
                        drugB,
                        true,
                        record.severity,
                        record.description,
                        record.sourceName + " [Development Mock Data]",
                        record.sourceRef
                ));
            }
        }

        return Optional.empty();
    }

    private String normalizeName(String s) {
        String clean = s.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        if (clean.contains("acetylsalicylic") || clean.contains("aspirin") || clean.equals("asa")) {
            return "aspirin";
        }
        if (clean.contains("warfarin") || clean.contains("coumadin")) {
            return "warfarin";
        }
        if (clean.contains("paracetamol") || clean.contains("acetaminophen") || clean.contains("tylenol") || clean.contains("panadol")) {
            return "paracetamol";
        }
        if (clean.contains("ibuprofen") || clean.contains("advil") || clean.contains("motrin")) {
            return "ibuprofen";
        }
        if (clean.contains("lisinopril") || clean.contains("prinivil") || clean.contains("zestril")) {
            return "lisinopril";
        }
        if (clean.contains("spironolactone") || clean.contains("aldactone")) {
            return "spironolactone";
        }
        if (clean.contains("simvastatin") || clean.contains("zocor")) {
            return "simvastatin";
        }
        if (clean.contains("clarithromycin") || clean.contains("biaxin")) {
            return "clarithromycin";
        }
        if (clean.contains("ciprofloxacin") || clean.contains("cipro")) {
            return "ciprofloxacin";
        }
        if (clean.contains("methotrexate")) {
            return "methotrexate";
        }
        return clean;
    }

    private static class InteractionRecord {
        final String drugA;
        final String drugB;
        final String severity;
        final String description;
        final String sourceName;
        final String sourceRef;

        InteractionRecord(String drugA, String drugB, String severity, String description, String sourceName, String sourceRef) {
            this.drugA = drugA;
            this.drugB = drugB;
            this.severity = severity;
            this.description = description;
            this.sourceName = sourceName;
            this.sourceRef = sourceRef;
        }
    }
}
