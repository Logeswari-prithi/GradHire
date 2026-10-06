package com.gradhire.initializer;

import com.gradhire.entity.*;
import com.gradhire.entity.Role;
import com.gradhire.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createDefaultAdmin();
        createDefaultBatches();
    }

    private void createDefaultAdmin() {
        User admin = userRepository.findByUsername("Admin").orElse(new User());
        admin.setUsername("Admin");
        admin.setEmail("admin@gradhire.com");
        admin.setPassword(passwordEncoder.encode("Admin@12345"));
        admin.setRole(Role.ADMIN);
        admin.setFullName("Admin");
        admin.setEnabled(true);
        userRepository.save(admin);
    }

    private void createDefaultBatches() {
        int[] years = {2025, 2026, 2027};
        for (int year : years) {
            if (batchRepository.findByYear(year).isEmpty()) {
                batchRepository.save(Batch.builder()
                        .year(year)
                        .description("Batch of " + year)
                        .build());
                log.info("Created batch: {}", year);
            }
        }
    }
}
