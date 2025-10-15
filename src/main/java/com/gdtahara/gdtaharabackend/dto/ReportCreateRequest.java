// =================================================================
// File: src/main/java/com/gdtahara/gdtaharabackend/dto/ReportCreateRequest.java
// (ฉบับแก้ไข)
// =================================================================
package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReportCreateRequest {
    
    // **[ใหม่]** เพิ่ม Field ที่จำเป็น
    private String orderNumber;
    private LocalDate startDate;
    private LocalDate endDate;

    private Long machineId;
    private Long productId;
    private Integer targetQty;

}