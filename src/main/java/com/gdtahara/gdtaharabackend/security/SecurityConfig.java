package com.gdtahara.gdtaharabackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; 
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, AuthenticationProvider authenticationProvider) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error", "/error/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/test-api.html", "/favicon.ico", "/static/**").permitAll()
                .requestMatchers("/health", "/").permitAll()
                
                // แยก paths ให้ชัดเจน และเพิ่ม production paths
                .requestMatchers("/api/production/**").authenticated() // สำหรับ ProductionController
                .requestMatchers("/api/pc/production/**").authenticated() // สำหรับ PC production endpoints
                .requestMatchers("/api/pc/**").authenticated()         // สำหรับ PC endpoints อื่นๆ
                .requestMatchers("/api/reports/**").authenticated()    // สำหรับ ReportController
                .requestMatchers("/api/master-data/**").authenticated() // สำหรับ MasterDataController
                .requestMatchers("/api/cm-operator/**").authenticated() // สำหรับ CmOperatorController
                
                // เพิ่ม /api/technician/** สำหรับ TechnicianController
                .requestMatchers("/api/technician/**").hasAnyAuthority(
                    "ROLE_Technician", 
                    "ROLE_DataAdmin",
                    "ROLE_Production Control"
                )
                
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .anonymous(anonymous -> anonymous.disable())
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(accessDeniedHandler())
            )
            .headers(headers -> 
                headers.frameOptions(frameOptions -> 
                    frameOptions.sameOrigin()
                )
            );

        return http.build();
    }

    @Bean
    public AccessDeniedHandlerImpl accessDeniedHandler() {
        AccessDeniedHandlerImpl handler = new AccessDeniedHandlerImpl();
        handler.setErrorPage("/error/403");
        System.out.println("Access Denied: User does not have the required role or authority.");
        return handler;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // อัปเดต ports
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",  // Frontend (Vite)
            "http://localhost:8081",  // Backend (ตัวเอง)
            "http://localhost:3000"   // Frontend (React/Next.js)
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        configuration.setExposedHeaders(Arrays.asList("X-Total-Count", "X-Debug-Info", "X-Error-Message"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}