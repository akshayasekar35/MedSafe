package com.medsafe.config;

import com.medsafe.service.drugdata.DevelopmentDrugDataProvider;
import com.medsafe.service.drugdata.DrugDataProvider;
import com.medsafe.service.drugdata.EvidenceGroundedInteractionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DrugDataProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(DrugDataProviderConfig.class);

    @Bean
    @Primary
    public DrugDataProvider activeDrugDataProvider(
            @Value("${medsafe.drug-data.provider:production}") String providerMode,
            EvidenceGroundedInteractionEngine productionEngine,
            DevelopmentDrugDataProvider developmentProvider) {

        if ("development".equalsIgnoreCase(providerMode) || "test".equalsIgnoreCase(providerMode)) {
            log.warn(">>> WARNING: Using DevelopmentDrugDataProvider for automated testing. DO NOT USE IN PRODUCTION. <<<");
            return developmentProvider;
        }

        log.info("Active clinical drug data provider: EvidenceGroundedInteractionEngine (RxNorm & FDA Labeling)");
        return productionEngine;
    }
}
