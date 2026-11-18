package com.gdtahara.gdtaharabackend.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Defense-in-depth check at startup.
 * After S-1/S-2 the application.properties has NO insecure defaults,
 * but this validator catches operators who explicitly set:
 *   SPRING_DATASOURCE_USERNAME=sa, or
 *   SPRING_DATASOURCE_PASSWORD=tst123##, or
 *   leave JWT_SECRET as the legacy placeholder.
 * Expected dedicated user: 'gdpd' (or similar least-privilege account).
 */
@Component
public class SecurityCredentialValidator {

    private static final Logger logger = LoggerFactory.getLogger(SecurityCredentialValidator.class);

    private static final String DEFAULT_DB_PASSWORD = "tst123##";
    private static final String DEFAULT_DB_USERNAME = "sa";
    private static final String DEFAULT_JWT_PREFIX  = "REPLACE-THIS-WITH";

    @Value("${spring.datasource.username:}")
    private String dbUsername;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @PostConstruct
    public void validate() {
        boolean insecure = false;

        if (DEFAULT_DB_USERNAME.equalsIgnoreCase(dbUsername)) {
            logger.error("SECURITY [S-3] DB username is 'sa' (SA account). " +
                    "Create a dedicated low-privilege login and set SPRING_DATASOURCE_USERNAME.");
            insecure = true;
        }

        if (DEFAULT_DB_PASSWORD.equals(dbPassword)) {
            logger.error("SECURITY [S-1] DB password is the default 'tst123##'. " +
                    "Rotate the password and set SPRING_DATASOURCE_PASSWORD env var.");
            insecure = true;
        }

        if (jwtSecret.startsWith(DEFAULT_JWT_PREFIX)) {
            logger.error("SECURITY [S-2] JWT secret is the placeholder value committed to source. " +
                    "Generate a 64-char random key and set JWT_SECRET env var: " +
                    "openssl rand -hex 32");
            insecure = true;
        }

        if (insecure) {
            logger.warn("=============================================================");
            logger.warn("  INSECURE DEFAULTS DETECTED — DO NOT USE IN PRODUCTION     ");
            logger.warn("  Set env vars: SPRING_DATASOURCE_USERNAME,                 ");
            logger.warn("                SPRING_DATASOURCE_PASSWORD,                 ");
            logger.warn("                JWT_SECRET                                  ");
            logger.warn("=============================================================");
        }
    }
}
