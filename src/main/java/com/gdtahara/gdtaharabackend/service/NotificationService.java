package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.ProblemAlertViewDto;
import com.gdtahara.gdtaharabackend.model.ProblemAlert;
import com.gdtahara.gdtaharabackend.repository.ProblemAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private ProblemAlertRepository problemAlertRepository;

    @Transactional(readOnly = true)
    public List<ProblemAlertViewDto> getActiveAlerts() {
        return problemAlertRepository.findByStatusOrderByTimestampDesc("New").stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void acknowledgeAlert(Long alertId) {
        ProblemAlert alert = problemAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
        alert.setStatus("Acknowledged");
        problemAlertRepository.save(alert);
    }

    private ProblemAlertViewDto convertToDto(ProblemAlert alert) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return new ProblemAlertViewDto(
            alert.getId(),
            alert.getReport().getMachine().getMachineName(),
            alert.getReport().getProduct().getProductName(),
            alert.getMessage(),
            alert.getOperator().getUsername(),
            alert.getTimestamp().format(formatter)
        );
    }
}