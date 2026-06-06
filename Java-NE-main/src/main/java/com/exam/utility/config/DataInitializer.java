package com.exam.utility.config;

import com.exam.utility.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository.findByEmail("admin@wasac-reg.rw").ifPresent(admin -> {
            if (!passwordEncoder.matches("Admin@1234!", admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode("Admin@1234!"));
                userRepository.save(admin);
                log.info("Admin password re-encoded and saved");
            }
        });
    }
}
