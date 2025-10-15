// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/repository/ProductRepository.java
// (ตรวจสอบว่ามีเมธอด findByProductCode)
// =================================================================
package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByProductCode(String productCode);
}