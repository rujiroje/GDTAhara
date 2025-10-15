// =================================================================
// File: repository/UserRepository.java
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // เพิ่ม: Method สำหรับค้นหาผู้ใช้ด้วย Username
    Optional<User> findByUsername(String username);
    
    // เพิ่ม: Method สำหรับค้นหาผู้ใช้ด้วย Role
    List<User> findByRole(String role);
}
