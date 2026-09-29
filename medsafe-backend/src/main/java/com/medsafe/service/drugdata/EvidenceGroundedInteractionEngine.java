package com.medsafe.service.drugdata;

import com.medsafe.service.drugdata.cache.DrugInformationCacheService;
import com.medsafe.service.drugdata.model.EvidenceStatus;
import com.medsafe.service.drugdata.openfda.OpenFdaDrugDataProvider;
import com.medsafe.service.drugdata.rxnorm.RxNormDrugDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class EvidenceGroundedInteractionEngine implements DrugDataProvider {

    private static final Logger log = LoggerFactory.getLogger(EvidenceGroundedInteractionEngine.class);
    public static final String PROVIDER_NAME = "Evidence-Grounded Clinical Knowledgebase (RxNorm & FDA Labeling)";

    private final DrugInformationCacheService cacheService;
    private final OpenFdaDrugDataProvider openFdaProvider;
    private final RxNormDrugDataProvider rxNormProvider;

    public EvidenceGroundedInteractionEngine(DrugInformationCacheService cacheService,
                                             OpenFdaDrugDataProvider openFdaProvider,
                                             RxNormDrugDataProvider rxNormProvider) {
        this.cacheService = cacheService;
        this.openFdaProvider = openFdaProvider;
        this.rxNormProvider = rxNormProvider;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public DrugInfoResult identifyDrug(String medicineName) {
        return cacheService.getOrFetchDrugInfo(medicineName);
    }

    @Override
    public Optional<DrugInteractionResult> checkInteraction(String drugA, String drugB) {
        if (drugA == null || drugB == null) {
            return Optional.empty();
        }

        // 1. Resolve drug concepts and active ingredients with cache & RxNorm
        DrugInfoResult infoA = cacheService.getOrFetchDrugInfo(drugA);
        DrugInfoResult infoB = cacheService.getOrFetchDrugInfo(drugB);

        // Check for ambiguous drug concepts
        if (infoA != null && infoA.getEvidenceStatus() == EvidenceStatus.AMBIGUOUS) {
            return Optional.of(DrugInteractionResult.ambiguous(drugA, drugB, infoA.getAmbiguousCandidates()));
        }
        if (infoB != null && infoB.getEvidenceStatus() == EvidenceStatus.AMBIGUOUS) {
            return Optional.of(DrugInteractionResult.ambiguous(drugA, drugB, infoB.getAmbiguousCandidates()));
        }

        // Check for external source unavailability during normalization
        if ((infoA != null && infoA.getEvidenceStatus() == EvidenceStatus.SOURCE_UNAVAILABLE) ||
            (infoB != null && infoB.getEvidenceStatus() == EvidenceStatus.SOURCE_UNAVAILABLE)) {
            return Optional.of(DrugInteractionResult.sourceUnavailable(drugA, drugB, "NLM RxNorm"));
        }

        // Check if both drugs represent the SAME active ingredient / substance (Case D: Duplicate Active Ingredient)
        if (isSameActiveSubstance(drugA, drugB, infoA, infoB)) {
            log.info("Pair '{}' and '{}' share the same active ingredient. Classified as duplicate therapy rather than drug-drug interaction.", drugA, drugB);
            DrugInteractionResult duplicateNotice = DrugInteractionResult.notFound(drugA, drugB, OpenFdaDrugDataProvider.SOURCE_NAME);
            duplicateNotice.setDescription("Both medications share the same active substance. This represents duplicate medication therapy rather than a distinct drug-to-drug interaction.");
            enrichTraceabilityMetadata(duplicateNotice, infoA, infoB);
            return Optional.of(duplicateNotice);
        }

        // 2. Prepare comprehensive clinical search terms (brand names, active ingredients, generic names)
        List<String> termsA = buildSearchTerms(drugA, infoA);
        List<String> termsB = buildSearchTerms(drugB, infoB);

        log.debug("Screening interaction between termsA={} and termsB={}", termsA, termsB);

        // 3. Screen evidence against official FDA drug labeling
        Optional<DrugInteractionResult> fdaResult = openFdaProvider.checkInteraction(drugA, drugB, termsA, termsB);
        if (fdaResult.isPresent()) {
            DrugInteractionResult res = fdaResult.get();
            enrichTraceabilityMetadata(res, infoA, infoB);
            return Optional.of(res);
        }

        // 4. If no interaction identified in the consulted source:
        // Strictly return NOT_FOUND without asserting the combination is 100% safe
        DrugInteractionResult notFoundResult = DrugInteractionResult.notFound(drugA, drugB, OpenFdaDrugDataProvider.SOURCE_NAME);
        enrichTraceabilityMetadata(notFoundResult, infoA, infoB);
        return Optional.of(notFoundResult);
    }

    private boolean isSameActiveSubstance(String drugA, String drugB, DrugInfoResult infoA, DrugInfoResult infoB) {
        if (drugA.trim().equalsIgnoreCase(drugB.trim())) {
            return true;
        }
        if (infoA != null && infoB != null) {
            if (infoA.getExternalId() != null && !infoA.getExternalId().isBlank()
                    && infoA.getExternalId().equals(infoB.getExternalId())) {
                return true;
            }
            if (infoA.getActiveIngredient() != null && infoB.getActiveIngredient() != null
                    && !infoA.getActiveIngredient().isBlank()
                    && infoA.getActiveIngredient().equalsIgnoreCase(infoB.getActiveIngredient())) {
                return true;
            }
        }
        return false;
    }

    private void enrichTraceabilityMetadata(DrugInteractionResult result, DrugInfoResult infoA, DrugInfoResult infoB) {
        if (result == null) return;
        if (infoA != null) {
            result.setRxcuiA(infoA.getExternalId());
            result.setActiveIngredientA(infoA.getActiveIngredient());
            result.setStandardNameA(infoA.getStandardName());
        }
        if (infoB != null) {
            result.setRxcuiB(infoB.getExternalId());
            result.setActiveIngredientB(infoB.getActiveIngredient());
            result.setStandardNameB(infoB.getStandardName());
        }
    }

    public List<String> buildSearchTerms(String originalName, DrugInfoResult info) {
        Set<String> terms = new LinkedHashSet<>();
        addCleanTermAndMoieties(terms, originalName);

        if (info != null) {
            addCleanTermAndMoieties(terms, info.getStandardName());
            if (info.getActiveIngredient() != null && !info.getActiveIngredient().isBlank()) {
                // Split multi-ingredient delimiters: comma, slash, plus
                for (String ing : info.getActiveIngredient().split("[,/+]")) {
                    addCleanTermAndMoieties(terms, ing);
                }
            }
        }
        return new ArrayList<>(terms);
    }

    private void addCleanTermAndMoieties(Set<String> terms, String candidate) {
        if (candidate == null) return;
        addCleanTerm(terms, candidate);
        String stripped = stripSaltSuffix(candidate);
        if (!stripped.equalsIgnoreCase(candidate)) {
            addCleanTerm(terms, stripped);
        }
    }

    private void addCleanTerm(Set<String> terms, String candidate) {
        if (candidate == null) return;
        String clean = candidate.trim().toLowerCase();
        if (clean.length() >= 3 && !OpenFdaDrugDataProvider.CLINICAL_STOPWORDS.contains(clean)) {
            terms.add(candidate.trim());
        }
    }

    private String stripSaltSuffix(String ingredient) {
        String clean = ingredient.trim();
        String lower = clean.toLowerCase();
        for (String suffix : Arrays.asList(" sodium", " potassium", " calcium", " magnesium", " hydrochloride", " hcl",
                " tartrate", " succinate", " maleate", " fumarate", " mesylate", " besylate", " acetate",
                " phosphate", " sulfate", " monohydrate", " dihydrate", " trihydrate")) {
            if (lower.endsWith(suffix)) {
                return clean.substring(0, clean.length() - suffix.length()).trim();
            }
        }
        return clean;
    }
}
