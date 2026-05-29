// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/security/UserDetailsServiceImpl.java
// (ไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.security;

import com.gdtahara.gdtaharabackend.model.User;
import com.gdtahara.gdtaharabackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Attempting to load user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", username);
                    return new UsernameNotFoundException("ไม่พบผู้ใช้งานชื่อ: " + username);
                });

        return user;
    }
}