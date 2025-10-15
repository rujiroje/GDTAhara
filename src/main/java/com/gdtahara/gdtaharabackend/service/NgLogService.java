// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/service/NgLogService.java
// (สร้างไฟล์ใหม่)
// =================================================================
package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.NgLogRequestDto;
import com.gdtahara.gdtaharabackend.model.NgLog;
import com.gdtahara.gdtaharabackend.repository.NgLogRepository;
import com.gdtahara.gdtaharabackend.repository.NgTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NgLogService {

    @Autowired
    private NgLogRepository ngLogRepository;

    @Autowired
    private NgTypeRepository ngTypeRepository;

    public NgLog updateNgLog(Long id, NgLogRequestDto request) {
        NgLog ngLog = ngLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("NG Log not found with id: " + id));
        
        ngLog.setNgType(ngTypeRepository.findById(request.getNgTypeId())
                .orElseThrow(() -> new EntityNotFoundException("NG Type not found")));
        ngLog.setQuantity(request.getQuantity());

        return ngLogRepository.save(ngLog);
    }

    public void deleteNgLog(Long id) {
        if (!ngLogRepository.existsById(id)) {
            throw new EntityNotFoundException("NG Log not found with id: " + id);
        }
        ngLogRepository.deleteById(id);
    }
}