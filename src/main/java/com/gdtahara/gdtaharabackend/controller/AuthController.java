// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/controller/AuthController.java
// (วางทับไฟล์เดิม)
// =================================================================
package com.gdtahara.gdtaharabackend.controller;

import com.gdtahara.gdtaharabackend.dto.AuthResponse;
import com.gdtahara.gdtaharabackend.dto.LoginRequest;
import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import com.gdtahara.gdtaharabackend.security.JwtUtil;
import com.gdtahara.gdtaharabackend.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // ยืนยันตัวตน
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        // ดึงข้อมูล UserDetails ที่สมบูรณ์ (ซึ่งมี Authorities)
        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
        
        // สร้าง Token จาก UserDetails
        final String jwt = jwtUtil.generateToken(userDetails);
        
        // ดึง Role จาก Authorities เพื่อส่งกลับไปให้ Frontend (โดยตัด ROLE_ ออก)
        String role = userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        return ResponseEntity.ok(new AuthResponse(jwt, role, userDetails.getUsername()));
    }
    
    @PostMapping("/register-initial-users")
    public ResponseEntity<?> registerInitialUsers() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setRole("ADMIN"); // เปลี่ยนเป็น ADMIN แทน DataAdmin
            userRepository.save(admin);
        }
        if (userRepository.findByUsername("operator").isEmpty()) {
            User operator = new User();
            operator.setUsername("operator");
            operator.setPassword(passwordEncoder.encode("password123"));
            operator.setRole("Operator"); // Role ที่นี่ไม่ต้องมี prefix
            userRepository.save(operator);
        }
        if (userRepository.findByUsername("pc").isEmpty()) {
            User pc = new User();
            pc.setUsername("pc");
            pc.setPassword(passwordEncoder.encode("password123"));
            pc.setRole("Production Control");
            userRepository.save(pc);
        }
        // เพิ่มผู้ใช้สำหรับบทบาทอ่านอย่างเดียว: Document และ Management
        if (userRepository.findByUsername("document").isEmpty()) {
            User doc = new User();
            doc.setUsername("document");
            doc.setPassword(passwordEncoder.encode("password123"));
            doc.setRole("Document");
            userRepository.save(doc);
        }
        if (userRepository.findByUsername("management").isEmpty()) {
            User mgmt = new User();
            mgmt.setUsername("management");
            mgmt.setPassword(passwordEncoder.encode("password123"));
            mgmt.setRole("Management");
            userRepository.save(mgmt);
        }
        return ResponseEntity.ok("สร้างผู้ใช้เริ่มต้น (admin, operator, pc, document, management) สำเร็จ!");
    }
}