package com.gdtahara.gdtaharabackend.dto;

<<<<<<< HEAD
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ProductionReportSimpleViewDto {
    private Long id;
    private String orderNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String machineName;
    private String productName;
    private String machineId;

    public ProductionReportSimpleViewDto(Long id, LocalDate startDate, String machineName, String productName) {
        this.id = id;
        this.startDate = startDate;
        this.machineName = machineName;
        this.productName = productName;
    }

    public ProductionReportSimpleViewDto(Long id, LocalDate startDate, String machineName, String productName, String machineId) {
        this(id, startDate, machineName, productName);
        this.machineId = machineId;
    }

    public ProductionReportSimpleViewDto(Long id, String orderNumber, LocalDate startDate, LocalDate endDate,
                                          String machineName, String productName, String machineId) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        this.machineName = machineName;
        this.productName = productName;
        this.machineId = machineId;
    }
=======
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; // 1. เพิ่ม NoArgsConstructor
import java.time.LocalDate;

@Data
@AllArgsConstructor // 2. AllArgsConstructor จะสร้าง Constructor ให้เราอัตโนมัติ
@NoArgsConstructor  // 3. เพิ่ม NoArgsConstructor เพื่อให้มีความยืดหยุ่น
public class ProductionReportSimpleViewDto {
    private Long id; // แก้ไขจากไฟล์เดิม อาจจะต้องเพิ่ม ID เข้ามา
    private LocalDate startDate;
    private String machineName;
    private String productName;
>>>>>>> 6f6d8f0e8ca1272ce48104c687b482d938aaade8
}