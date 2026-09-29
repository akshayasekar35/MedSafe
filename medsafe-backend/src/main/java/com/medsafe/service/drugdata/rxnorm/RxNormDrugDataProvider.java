package com.medsafe.service.drugdata.rxnorm;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.medsafe.service.drugdata.DrugInfoResult;
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

import java.time.Duration;
import java.util.*;

@Service
public class RxNormDrugDataProvider {

    private static final Logger log = LoggerFactory.getLogger(RxNormDrugDataProvider.class);
    public static final String SOURCE_NAME = "NLM RxNorm API";

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RxNormDrugDataProvider(
            @Value("${medsafe.rxnorm.base-url:https://rxnav.nlm.nih.gov/REST}") String baseUrl,
            @Value("${medsafe.rxnorm.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${medsafe.rxnorm.read-timeout-ms:5000}") int readTimeoutMs) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Identifies a drug by name using exact match, normalized match, or approximate concept search.
     */
    public DrugInfoResult identifyDrug(String medicineName) {
        if (medicineName == null || medicineName.trim().isEmpty()) {
            return DrugInfoResult.notFound("");
        }

        String clean = medicineName.trim();

        try {
            // Step 1: Exact search
            String exactRxcui = findExactRxcui(clean);
            if (exactRxcui != null) {
                return buildDrugInfo(exactRxcui, clean);
            }

            // Step 2: Normalized search
            String normalizedRxcui = findNormalizedRxcui(clean);
            if (normalizedRxcui != null) {
                return buildDrugInfo(normalizedRxcui, clean);
            }

            // Step 3: Approximate term search & ambiguity check
            return findApproximateOrAmbiguous(clean);

        } catch (ResourceAccessException ex) {
            log.warn("RxNorm API connection timeout or unreachable: {}", ex.getMessage());
            return DrugInfoResult.unavailable(clean);
        } catch (RestClientException ex) {
            log.warn("RxNorm REST client error for '{}': {}", clean, ex.getMessage());
            return DrugInfoResult.unavailable(clean);
        } catch (Exception ex) {
            log.error("Unexpected error querying RxNorm for '{}': {}", clean, ex.getMessage(), ex);
            return DrugInfoResult.unavailable(clean);
        }
    }

    private String findExactRxcui(String name) {
        try {
            String uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .path("/rxcui.json")
                    .queryParam("name", name)
                    .queryParam("search", 0)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode rxnormIdArray = root.path("idGroup").path("rxnormId");
                if (rxnormIdArray.isArray() && !rxnormIdArray.isEmpty()) {
                    return rxnormIdArray.get(0).asText();
                }
            }
        } catch (ResourceAccessException ex) {
            throw ex;
        } catch (Exception e) {
            log.debug("Exact RxCUI lookup failed for '{}': {}", name, e.getMessage());
        }
        return null;
    }

    private String findNormalizedRxcui(String name) {
        try {
            String uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .path("/rxcui.json")
                    .queryParam("name", name)
                    .queryParam("search", 1)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode rxnormIdArray = root.path("idGroup").path("rxnormId");
                if (rxnormIdArray.isArray() && !rxnormIdArray.isEmpty()) {
                    return rxnormIdArray.get(0).asText();
                }
            }
        } catch (ResourceAccessException ex) {
            throw ex;
        } catch (Exception e) {
            log.debug("Normalized RxCUI lookup failed for '{}': {}", name, e.getMessage());
        }
        return null;
    }

    private DrugInfoResult findApproximateOrAmbiguous(String name) {
        try {
            String uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .path("/approximateTerm.json")
                    .queryParam("term", name)
                    .queryParam("maxEntries", 5)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return DrugInfoResult.notFound(name);
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode candidates = root.path("approximateGroup").path("candidate");

            if (!candidates.isArray() || candidates.isEmpty()) {
                return DrugInfoResult.notFound(name);
            }

            // Collect unique distinct RxCUIs
            Set<String> distinctRxcuis = new LinkedHashSet<>();
            for (JsonNode c : candidates) {
                String rxcui = c.path("rxcui").asText();
                if (rxcui != null && !rxcui.isBlank()) {
                    distinctRxcuis.add(rxcui);
                }
            }

            if (distinctRxcuis.isEmpty()) {
                return DrugInfoResult.notFound(name);
            }

            // If only 1 distinct concept was returned, use it
            if (distinctRxcuis.size() == 1) {
                String singleRxcui = distinctRxcuis.iterator().next();
                return buildDrugInfo(singleRxcui, name);
            }

            // Multiple distinct concepts returned: retrieve their standard names to check for ambiguity
            List<String> candidateNames = new ArrayList<>();
            for (String rxcui : distinctRxcuis) {
                String propName = getRxNormName(rxcui);
                if (propName != null && !propName.isBlank()) {
                    candidateNames.add(propName + " (RxCUI: " + rxcui + ")");
                } else {
                    candidateNames.add("RxCUI " + rxcui);
                }
                if (candidateNames.size() >= 4) break;
            }

            log.info("Ambiguous RxNorm match for '{}': candidates={}", name, candidateNames);
            return DrugInfoResult.ambiguous(name, candidateNames);

        } catch (ResourceAccessException ex) {
            throw ex;
        } catch (Exception e) {
            log.warn("Approximate lookup failed for '{}': {}", name, e.getMessage());
            return DrugInfoResult.notFound(name);
        }
    }

    private DrugInfoResult buildDrugInfo(String rxcui, String originalName) {
        String standardName = getRxNormName(rxcui);
        if (standardName == null || standardName.isBlank()) {
            standardName = originalName;
        }

        String activeIngredient = getActiveIngredients(rxcui);
        if (activeIngredient == null || activeIngredient.isBlank()) {
            activeIngredient = standardName;
        }

        String drugClass = getDrugClass(rxcui);

        return new DrugInfoResult(
                standardName,
                activeIngredient,
                drugClass != null ? drugClass : "General Therapeutic Agent",
                SOURCE_NAME,
                rxcui,
                EvidenceStatus.VERIFIED
        );
    }

    public String getRxNormName(String rxcui) {
        try {
            String uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .path("/rxcui/" + rxcui + "/allProperties.json")
                    .queryParam("prop", "names")
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode propArray = root.path("propConceptGroup").path("propConcept");
                if (propArray.isArray()) {
                    for (JsonNode prop : propArray) {
                        if ("RxNorm Name".equalsIgnoreCase(prop.path("propName").asText())) {
                            return prop.path("propValue").asText();
                        }
                    }
                    if (!propArray.isEmpty()) {
                        return propArray.get(0).path("propValue").asText();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get RxNorm Name for rxcui '{}': {}", rxcui, e.getMessage());
        }
        return null;
    }

    public String getActiveIngredients(String rxcui) {
        try {
            String uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .path("/rxcui/" + rxcui + "/related.json")
                    .queryParam("tty", "IN+PIN")
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode conceptGroups = root.path("relatedGroup").path("conceptGroup");
                if (conceptGroups.isArray()) {
                    List<String> ingredients = new ArrayList<>();
                    for (JsonNode cg : conceptGroups) {
                        JsonNode props = cg.path("conceptProperties");
                        if (props.isArray()) {
                            for (JsonNode p : props) {
                                String inName = p.path("name").asText();
                                if (inName != null && !inName.isBlank() && !ingredients.contains(inName)) {
                                    ingredients.add(inName);
                                }
                            }
                        }
                    }
                    if (!ingredients.isEmpty()) {
                        return String.join(" / ", ingredients);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get active ingredient for rxcui '{}': {}", rxcui, e.getMessage());
        }
        return null;
    }

    public String getDrugClass(String rxcui) {
        try {
            String uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .path("/rxclass/class/byRxcui.json")
                    .queryParam("rxcui", rxcui)
                    .queryParam("relaSource", "ATC")
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode classList = root.path("rxclassDrugInfoList").path("rxclassDrugInfo");
                if (classList.isArray() && !classList.isEmpty()) {
                    String className = classList.get(0).path("rxclassMinConceptItem").path("className").asText();
                    if (className != null && !className.isBlank()) {
                        return className;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get drug class for rxcui '{}': {}", rxcui, e.getMessage());
        }
        return null;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
