package com.medsafe.repository;

import com.medsafe.entity.DrugInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface DrugInformationRepository extends JpaRepository<DrugInformation, Long> {
    Optional<DrugInformation> findByStandardNameIgnoreCase(String standardName);
    List<DrugInformation> findByActiveIngredientIgnoreCase(String activeIngredient);
}
