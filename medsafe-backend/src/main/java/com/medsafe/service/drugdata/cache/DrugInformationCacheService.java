package com.medsafe.service.drugdata.cache;

import com.medsafe.entity.DrugInformation;
import com.medsafe.repository.DrugInformationRepository;
import com.medsafe.service.drugdata.DrugInfoResult;
import com.medsafe.service.drugdata.model.EvidenceStatus;
import com.medsafe.service.drugdata.rxnorm.RxNormDrugDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class DrugInformationCacheService {

    private static final Logger log = LoggerFactory.getLogger(DrugInformationCacheService.class);

    private final DrugInformationRepository drugInformationRepository;
    private final RxNormDrugDataProvider rxNormDrugDataProvider;

    public DrugInformationCacheService(DrugInformationRepository drugInformationRepository,
                                       RxNormDrugDataProvider rxNormDrugDataProvider) {
        this.drugInformationRepository = drugInformationRepository;
        this.rxNormDrugDataProvider = rxNormDrugDataProvider;
    }

    /**
     * Checks database cache first to avoid repeated external API calls.
     * If not found, fetches from RxNorm and stores in the drug_information table.
     */
    @Transactional
    public DrugInfoResult getOrFetchDrugInfo(String medicineName) {
        if (medicineName == null || medicineName.trim().isEmpty()) {
            return DrugInfoResult.notFound("");
        }

        String clean = medicineName.trim();

        // 1. Check local DB cache
        Optional<DrugInformation> cached = drugInformationRepository.findByStandardNameIgnoreCase(clean);
        if (cached.isPresent()) {
            DrugInformation info = cached.get();
            log.debug("Database cache hit for drug '{}' (standard: {})", clean, info.getStandardName());
            return new DrugInfoResult(
                    info.getStandardName(),
                    info.getActiveIngredient(),
                    info.getDrugClass(),
                    info.getExternalSource() != null ? info.getExternalSource() : "RxNorm (Cached)",
                    info.getExternalId(),
                    EvidenceStatus.VERIFIED
            );
        }

        // 2. Fetch from external RxNorm API
        DrugInfoResult result = rxNormDrugDataProvider.identifyDrug(clean);

        // 3. Cache verified results
        if (result != null && result.getEvidenceStatus() == EvidenceStatus.VERIFIED && result.getExternalId() != null) {
            try {
                DrugInformation entity = new DrugInformation();
                entity.setStandardName(result.getStandardName() != null ? result.getStandardName() : clean);
                entity.setActiveIngredient(result.getActiveIngredient());
                entity.setDrugClass(result.getDrugClass());
                entity.setExternalSource(result.getExternalSource());
                entity.setExternalId(result.getExternalId());
                drugInformationRepository.save(entity);
                log.info("Saved drug info to cache: standardName='{}', rxcui='{}'", entity.getStandardName(), entity.getExternalId());
            } catch (Exception ex) {
                log.warn("Failed to persist drug info cache for '{}': {}", clean, ex.getMessage());
            }
        }

        return result;
    }

    public Optional<DrugInformation> findInCache(String medicineName) {
        if (medicineName == null || medicineName.trim().isEmpty()) {
            return Optional.empty();
        }
        return drugInformationRepository.findByStandardNameIgnoreCase(medicineName.trim());
    }
}
