package com.gdtahara.gdtaharabackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @GetMapping("/simple")
    public ResponseEntity<?> testSimple() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Test controller working");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}