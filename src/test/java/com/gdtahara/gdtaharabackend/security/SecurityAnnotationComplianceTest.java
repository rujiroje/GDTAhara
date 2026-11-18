package com.gdtahara.gdtaharabackend.security;

import com.gdtahara.gdtaharabackend.controller.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reflection-based compliance check: write-endpoint controllers must carry
 * a class-level or method-level @PreAuthorize annotation.
 *
 * This is a static analysis test — no Spring context, no DB.
 */
class SecurityAnnotationComplianceTest {

    @Test
    void writeEndpointControllers_havePreAuthorizeAtClassOrMethodLevel() {
        List<Class<?>> writeControllers = List.of(
                TechnicianController.class,
                ShiftLeaderController.class,
                OperatorController.class,
                CmOperatorController.class,
                EmergencyController.class,
                AdminController.class
        );

        List<String> violations = new ArrayList<>();

        for (Class<?> controller : writeControllers) {
            boolean classLevel = controller.isAnnotationPresent(PreAuthorize.class);

            if (!classLevel) {
                // Check every write method (POST/PUT/PATCH/DELETE)
                for (Method method : controller.getDeclaredMethods()) {
                    if (isWriteEndpoint(method) && !method.isAnnotationPresent(PreAuthorize.class)) {
                        violations.add(controller.getSimpleName() + "#" + method.getName()
                                + " — no @PreAuthorize (class-level or method-level)");
                    }
                }
            }
        }

        assertThat(violations)
                .as("Write endpoints without @PreAuthorize:\n" + String.join("\n", violations))
                .isEmpty();
    }

    @Test
    void productionControlController_hasPreAuthorize() {
        assertThat(ProductionControlController.class.isAnnotationPresent(PreAuthorize.class))
                .as("ProductionControlController must have class-level @PreAuthorize")
                .isTrue();
    }

    @Test
    void authController_hasNoPreAuthorize_isIntentional() {
        // Login endpoint must remain open — verify it has no PreAuthorize (i.e. it's in the permitAll list)
        assertThat(AuthController.class.isAnnotationPresent(PreAuthorize.class)).isFalse();
        for (Method m : AuthController.class.getDeclaredMethods()) {
            assertThat(m.isAnnotationPresent(PreAuthorize.class))
                    .as("AuthController." + m.getName() + " should NOT have @PreAuthorize — it must remain open")
                    .isFalse();
        }
    }

    private boolean isWriteEndpoint(Method method) {
        return method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }
}
