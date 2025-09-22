package com.gdtahara.gdtaharabackend.security;

import com.gdtahara.gdtaharabackend.repository.UserRepository;
import com.gdtahara.gdtaharabackend.util.EntityMethodsChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

@Configuration
@EnableMethodSecurity
public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private final UserRepository userRepository;

    public ApplicationConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    @SuppressWarnings("deprecation") // ปิด warning สำหรับ deprecated API
    public AuthenticationProvider authenticationProvider() {
        // ใช้ @SuppressWarnings เพื่อปิด warning ที่เกิดจาก deprecated constructor
        var authProvider = new org.springframework.security.authentication.dao.DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        // ใช้ ProviderManager แทนการใช้ AuthenticationConfiguration
        return new ProviderManager(Collections.singletonList(authenticationProvider()));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner debugEntityMethods(EntityMethodsChecker checker) {
        return args -> {
            // แสดงเมธอดที่มีใน ParameterRecord เมื่อ start application
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Starting Entity Methods Check...");
                    checker.printParameterRecordMethods();
                } else {
                    logger.info("Entity Methods Checker is available (enable DEBUG logging to see details)");
                }
            } catch (Exception e) {
                logger.warn("Could not run entity methods check: {}", e.getMessage());
            }
        };
    }
}