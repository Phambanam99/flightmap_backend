package com.phamnam.tracking_vessel_flight.config;

import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Value("${admin.email:admin@example.com}")
    private String adminEmail;

    @Override
    public void run(String... args) throws Exception {
        // Check if admin user already exists
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            // Create admin user
            User adminUser = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .email(adminEmail)
                    .role("ADMIN")
                    .build();

            userRepository.save(adminUser);

            System.out.println("Admin user created successfully!");
        } else {
            System.out.println("Admin user already exists, skipping initialization.");
        }
    }
}