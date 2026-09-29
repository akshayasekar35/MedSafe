package com.medsafe.service.drugdata;

import java.util.Optional;

public interface DrugDataProvider {

    String getProviderName();

    DrugInfoResult identifyDrug(String medicineName);

    Optional<DrugInteractionResult> checkInteraction(String drugA, String drugB);
}
