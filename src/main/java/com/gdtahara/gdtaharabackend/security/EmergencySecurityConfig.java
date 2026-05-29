package com.gdtahara.gdtaharabackend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "app.mode", havingValue = "emergency")
public class EmergencySecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(EmergencySecurityConfig.class);

    @PostConstruct
    public void warnEmergencyMode() {
        logger.warn("==========================================================");
        logger.warn("  EMERGENCY MODE IS ACTIVE (app.mode=emergency)");
        logger.warn("  /emergency/** endpoints are publicly accessible.");
        logger.warn("  All other endpoints still require authentication.");
        logger.warn("  NEVER run emergency mode in production.");
        logger.warn("==========================================================");
    }

    @Bean
    public SecurityFilterChain emergencySecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(emergencyCorsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/emergency/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .headers(headers ->
                headers.frameOptions(frameOptions ->
                    frameOptions.sameOrigin()
                )
            );

        return http.build();
    }

    @Bean
    CorsConfigurationSource emergencyCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:8080",
            "http://localhost:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
