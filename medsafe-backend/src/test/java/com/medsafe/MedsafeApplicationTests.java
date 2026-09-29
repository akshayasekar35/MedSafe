package com.medsafe;

import com.medsafe.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class MedsafeApplicationTests {

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

    @Autowired
    private DrugInformationRepository drugInformationRepository;

    @Test
    void contextLoadsAndRepositoriesConnect() {
        assertNotNull(userRepository);
        assertNotNull(medicationRepository);
        assertNotNull(scheduleRepository);
        assertNotNull(medicationLogRepository);
        assertNotNull(interactionAlertRepository);
        assertNotNull(drugInformationRepository);

        // Verify database connectivity and schema validation by querying counts
        userRepository.count();
        medicationRepository.count();
        scheduleRepository.count();
        medicationLogRepository.count();
        interactionAlertRepository.count();
        drugInformationRepository.count();
    }
}
