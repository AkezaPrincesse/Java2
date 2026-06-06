package com.exam.utility.config;

import com.exam.utility.entity.User;
import com.exam.utility.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
                return Optional.of("SYSTEM");
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                return Optional.of(user.getEmail());
            }
            return Optional.of(authentication.getName());
        };
    }
}
