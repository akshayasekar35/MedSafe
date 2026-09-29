package com.medsafe.service.drugdata.openfda;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.medsafe.service.drugdata.DrugInteractionResult;
import com.medsafe.service.drugdata.model.EvidenceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OpenFdaDrugDataProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenFdaDrugDataProvider.class);
    public static final String SOURCE_NAME = "FDA Drug Labeling (openFDA)";

    private final String baseUrl;
    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenFdaDrugDataProvider(
            @Value("${medsafe.openfda.base-url:https://api.fda.gov/drug/label.json}") String baseUrl,
            @Value("${medsafe.openfda.api-key:}") String apiKey,
            @Value("${medsafe.openfda.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${medsafe.openfda.read-timeout-ms:5000}") int readTimeoutMs) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey != null ? apiKey.trim() : "";

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Checks openFDA official drug labeling for documented drug interactions.
     * Note: openFDA labeling is an evidence source of package inserts, not an exhaustive interaction engine.
     */
    public Optional<DrugInteractionResult> checkInteraction(String drugA, String drugB, List<String> searchTermsA, List<String> searchTermsB) {
        if (drugA == null || drugB == null) {
            return Optional.empty();
        }

        try {
            // Check Label A for mentions of Drug B terms
            Optional<DrugInteractionResult> res = checkLabelForTarget(drugA, drugB, searchTermsA, searchTermsB);
            if (res.isPresent()) {
                return res;
            }

            // Reverse check: Label B for mentions of Drug A terms
            return checkLabelForTarget(drugB, drugA, searchTermsB, searchTermsA);

        } catch (ResourceAccessException ex) {
            log.warn("openFDA API timeout/unreachable for '{}' + '{}': {}", drugA, drugB, ex.getMessage());
            return Optional.of(DrugInteractionResult.sourceUnavailable(drugA, drugB, SOURCE_NAME));
        } catch (RestClientException ex) {
            log.warn("openFDA REST error for '{}' + '{}': {}", drugA, drugB, ex.getMessage());
            return Optional.of(DrugInteractionResult.sourceUnavailable(drugA, drugB, SOURCE_NAME));
        } catch (Exception ex) {
            log.error("Unexpected error querying openFDA: {}", ex.getMessage(), ex);
            return Optional.of(DrugInteractionResult.sourceUnavailable(drugA, drugB, SOURCE_NAME));
        }
    }

    public static final Set<String> CLINICAL_STOPWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "sodium", "potassium", "calcium", "magnesium", "chloride", "hydrochloride", "hcl",
            "acid", "tablet", "tablets", "capsule", "capsules", "solution", "suspension",
            "oral", "mg", "ml", "extended", "release", "tartrate", "succinate", "maleate",
            "fumarate", "mesylate", "besylate", "acetate", "phosphate", "sulfate", "hydrate",
            "anhydrous", "injection", "topical", "cream", "ointment", "dose", "daily",
            "drug", "drugs", "medicine", "medication", "agent", "agents", "therapy", "use",
            "with", "and", "the", "for", "other", "label", "oral tablet", "oral capsule",
            "warning", "warnings", "caution", "precautions", "interaction", "interactions"
    )));

    private Optional<DrugInteractionResult> checkLabelForTarget(String primaryDrug, String secondaryDrug, List<String> primaryTerms, List<String> targetTerms) {
        // Collect normalized primary terms so we NEVER match the primary drug's own name in its own label
        Set<String> normalizedPrimary = new HashSet<>();
        for (String p : primaryTerms) {
            if (p != null && !p.isBlank()) {
                normalizedPrimary.add(p.trim().toLowerCase());
            }
        }

        for (String pTerm : primaryTerms) {
            if (pTerm == null || pTerm.isBlank()) continue;
            String cleanPrimary = pTerm.trim().toLowerCase();
            if (CLINICAL_STOPWORDS.contains(cleanPrimary)) continue;

            JsonNode labelResult = queryLabel(pTerm);
            if (labelResult == null) continue;

            JsonNode interactionsNode = labelResult.path("drug_interactions");
            if (!interactionsNode.isArray() || interactionsNode.isEmpty()) continue;

            StringBuilder fullTextBuilder = new StringBuilder();
            for (JsonNode textNode : interactionsNode) {
                fullTextBuilder.append(textNode.asText()).append(" ");
            }
            String interactionText = fullTextBuilder.toString();

            // Search for target terms in interaction text
            for (String tTerm : targetTerms) {
                if (tTerm == null || tTerm.trim().length() < 3) continue;
                String normalizedTarget = tTerm.trim().toLowerCase();

                // CRITICAL SAFETY CHECK: Never match primary drug's own name, and never match clinical stopwords
                if (normalizedPrimary.contains(normalizedTarget) || CLINICAL_STOPWORDS.contains(normalizedTarget)) {
                    continue;
                }

                if (containsWord(interactionText, normalizedTarget)) {
                    String snippet = extractSnippet(interactionText, normalizedTarget);
                    String severity = extractSeverityOrUnspecified(snippet);

                    // Retrieve FDA label source reference
                    String sourceRef = extractSourceReference(labelResult);

                    DrugInteractionResult result = new DrugInteractionResult(
                            primaryDrug,
                            secondaryDrug,
                            true,
                            severity,
                            snippet,
                            SOURCE_NAME,
                            sourceRef,
                            EvidenceStatus.VERIFIED
                    );
                    result.setRetrievedAt(LocalDateTime.now());
                    log.info("openFDA verified interaction: '{}' + '{}' (Ref: {})", primaryDrug, secondaryDrug, sourceRef);
                    return Optional.of(result);
                }
            }
        }
        return Optional.empty();
    }

    private JsonNode queryLabel(String drugName) {
        try {
            String clean = drugName.trim().replaceAll("\"", "");
            // Search by generic_name, brand_name, or substance_name
            String searchQuery = String.format("openfda.generic_name:\"%s\"+OR+openfda.brand_name:\"%s\"+OR+openfda.substance_name:\"%s\"", clean, clean, clean);

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("search", searchQuery)
                    .queryParam("limit", 1);

            if (!apiKey.isEmpty()) {
                uriBuilder.queryParam("api_key", apiKey);
            }

            String uri = uriBuilder.build(false).toUriString();
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode results = root.path("results");
                if (results.isArray() && !results.isEmpty()) {
                    return results.get(0);
                }
            }
        } catch (ResourceAccessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.debug("No openFDA label found for '{}': {}", drugName, ex.getMessage());
        }
        return null;
    }

    private boolean containsWord(String fullText, String word) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(fullText).find();
    }

    private String extractSnippet(String fullText, String keyword) {
        int index = fullText.toLowerCase().indexOf(keyword.toLowerCase());
        if (index < 0) {
            return fullText.length() > 300 ? fullText.substring(0, 300) + "..." : fullText;
        }

        int start = Math.max(0, index - 120);
        int end = Math.min(fullText.length(), index + keyword.length() + 250);

        String snippet = fullText.substring(start, end).trim();
        // Clean up leading/trailing partial words if not at edge
        if (start > 0) snippet = "..." + snippet;
        if (end < fullText.length()) snippet = snippet + "...";

        return snippet.replaceAll("\\s+", " ");
    }

    /**
     * Strict Evidence-Grounded Severity Extraction.
     * 
     * Rule: We NEVER invent severity tiers. Words like "caution", "warning", "fatal", or "interaction"
     * are never converted into MAJOR/MODERATE/MINOR tiers. Only explicit clinical severity designations
     * in the source are preserved; otherwise returns DOCUMENTED_IN_LABEL.
     */
    public String extractSeverityOrUnspecified(String snippet) {
        if (snippet == null) {
            return "DOCUMENTED_IN_LABEL";
        }
        String lower = snippet.toLowerCase();

        // 1. Explicit contraindications
        if (Pattern.compile("\\bcontraindicated\\b", Pattern.CASE_INSENSITIVE).matcher(lower).find()
                || Pattern.compile("\\bcontraindication\\b", Pattern.CASE_INSENSITIVE).matcher(lower).find()) {
            return "CONTRAINDICATED";
        }

        // 2. Explicit tier designations stated as "major", "severe", "moderate", "minor"
        // Note: Do NOT match general clinical words like "caution", "warning", "risk", "hazard"
        if (Pattern.compile("\\bmajor\\b", Pattern.CASE_INSENSITIVE).matcher(lower).find()
                || Pattern.compile("\\bsevere\\b", Pattern.CASE_INSENSITIVE).matcher(lower).find()) {
            return "MAJOR";
        }
        if (Pattern.compile("\\bmoderate\\b", Pattern.CASE_INSENSITIVE).matcher(lower).find()) {
            return "MODERATE";
        }
        if (Pattern.compile("\\bminor\\b", Pattern.CASE_INSENSITIVE).matcher(lower).find()) {
            return "MINOR";
        }

        // When the label describes an interaction without formal discrete tiering:
        return "DOCUMENTED_IN_LABEL";
    }

    private String extractSourceReference(JsonNode labelResult) {
        JsonNode openfda = labelResult.path("openfda");
        JsonNode splSetId = openfda.path("spl_set_id");
        if (splSetId.isArray() && !splSetId.isEmpty()) {
            return "SPL Set ID: " + splSetId.get(0).asText();
        }

        JsonNode appNum = openfda.path("application_number");
        if (appNum.isArray() && !appNum.isEmpty()) {
            return "FDA Application: " + appNum.get(0).asText();
        }

        String setId = labelResult.path("set_id").asText();
        if (setId != null && !setId.isBlank()) {
            return "FDA Set ID: " + setId;
        }

        return "FDA Drug Labeling Reference";
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
