package com.gdtahara.gdtaharabackend.security;

import jakarta.servlet.http.HttpServletResponse;
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
import com.gdtahara.gdtaharabackend.filter.AuditRequestFilter;
import com.gdtahara.gdtaharabackend.filter.LoginRateLimitFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final AuditRequestFilter auditRequestFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, AuthenticationProvider authenticationProvider,
                          AuditRequestFilter auditRequestFilter,
                          LoginRateLimitFilter loginRateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
        this.auditRequestFilter = auditRequestFilter;
        this.loginRateLimitFilter = loginRateLimitFilter;
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
                // Actuator: health for load-balancer; prometheus for Prometheus scraper
                // Restrict /actuator/prometheus at the network/firewall level in production
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                // All other actuator endpoints require authentication
                .requestMatchers("/actuator/**").authenticated()

                .requestMatchers("/api/production/**").authenticated()
                .requestMatchers("/api/pc/production/**").authenticated()
                .requestMatchers("/api/pc/**").authenticated()
                .requestMatchers("/api/reports/**").authenticated()
                .requestMatchers("/api/master-data/**").authenticated()
                .requestMatchers("/api/cm-operator/**").authenticated()

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
            .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(auditRequestFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .anonymous(anonymous -> anonymous.disable())
            .exceptionHandling(exceptions -> exceptions
                // 401 for unauthenticated / bad token — keeps it distinct from 403
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\"}");
                })
                // 403 for authenticated users lacking the required role
                .accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Forbidden\"}");
                })
            )
            .headers(headers ->
                headers.frameOptions(frameOptions ->
                    frameOptions.sameOrigin()
                )
            );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:8081",
            "http://localhost:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        configuration.addAllowedHeader("*");
        configuration.setExposedHeaders(Arrays.asList("X-Total-Count", "X-Debug-Info", "X-Error-Message"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
