package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {
}
