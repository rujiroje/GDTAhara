package com.gdtahara.gdtaharabackend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lightweight list view DTO for ProductionReport listings (admin table / search)
 * Keep only columns that UI displays; avoid loading full entity graph.
 */
public class ProductionReportListDto {
    private Long id;
    private String orderNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String machineName;
    private String productName;
    private LocalDateTime createdAt;

    public ProductionReportListDto(Long id, String orderNumber, LocalDate startDate, LocalDate endDate,
                                   String status, String machineName, String productName, LocalDateTime createdAt) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.machineName = machineName;
        this.productName = productName;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getStatus() { return status; }
    public String getMachineName() { return machineName; }
    public String getProductName() { return productName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
