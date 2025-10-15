package com.gdtahara.gdtaharabackend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.gdtahara.gdtaharabackend.model.User;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @GetMapping("/validate-token")
    public String validateToken(@RequestHeader("Authorization") String authorizationHeader) {
        return "Token received: " + authorizationHeader;
    }
    
    @GetMapping("/current-user")
    public Map<String, Object> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                response.put("username", user.getUsername());
                response.put("role", user.getRole());
                response.put("authorities", auth.getAuthorities());
            }
            response.put("principalType", principal.getClass().getSimpleName());
        }
        
        return response;
    }
}
