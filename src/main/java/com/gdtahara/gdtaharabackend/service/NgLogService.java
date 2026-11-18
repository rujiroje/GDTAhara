package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.NgLogRequestDto;
import com.gdtahara.gdtaharabackend.model.NgLog;
import com.gdtahara.gdtaharabackend.repository.NgLogRepository;
import com.gdtahara.gdtaharabackend.repository.NgTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class NgLogService {

    @Autowired private NgLogRepository ngLogRepository;
    @Autowired private NgTypeRepository ngTypeRepository;
    @Autowired private AuditLogService auditLogService;

    public NgLog updateNgLog(Long id, NgLogRequestDto request, String username) {
        NgLog ngLog = ngLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("NG Log not found with id: " + id));

        if (!isAdminOrOwner(ngLog, username)) {
            throw new AccessDeniedException("Access denied: you do not own this record");
        }

        Integer oldQty = ngLog.getQuantity();
        Long oldTypeId = ngLog.getNgType() != null ? ngLog.getNgType().getId() : null;

        ngLog.setNgType(ngTypeRepository.findById(request.getNgTypeId())
                .orElseThrow(() -> new EntityNotFoundException("NG Type not found")));
        ngLog.setQuantity(request.getQuantity());

        NgLog saved = ngLogRepository.save(ngLog);
        auditLogService.log("UPDATE", "NgLog", id,
                Map.of("id", id, "quantity", String.valueOf(oldQty), "ngTypeId", String.valueOf(oldTypeId)),
                Map.of("id", id, "quantity", String.valueOf(saved.getQuantity()), "ngTypeId", String.valueOf(request.getNgTypeId())));
        return saved;
    }

    public void deleteNgLog(Long id, String username) {
        NgLog ngLog = ngLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("NG Log not found with id: " + id));

        if (!isAdminOrOwner(ngLog, username)) {
            throw new AccessDeniedException("Access denied: you do not own this record");
        }

        auditLogService.log("DELETE", "NgLog", id,
                Map.of("id", id, "quantity", String.valueOf(ngLog.getQuantity())), null);
        ngLogRepository.delete(ngLog);
    }

    private boolean isAdminOrOwner(NgLog ngLog, String username) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DataAdmin"));
        if (isAdmin) return true;
        return ngLog.getUser() != null && username.equals(ngLog.getUser().getUsername());
    }
}
