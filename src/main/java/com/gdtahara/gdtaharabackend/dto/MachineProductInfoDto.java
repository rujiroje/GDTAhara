package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO สำหรับข้อมูลเครื่องจักรและผลิตภัณฑ์
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineProductInfoDto {

    /**
     * หมายเลขใบสั่งผลิต
     */
    private String orderNumber;

    /**
     * ชื่อเครื่องจักร
     */
    private String machineName;

    /**
     * ชื่อผลิตภัณฑ์
     */
    private String productName;

    /**
     * เป้าหมายการผลิต (ชิ้น)
     */
    private Long targetQty;

    /**
     * รหัสผลิตภัณฑ์
     */
    private String productCode;

    /**
     * รหัสเครื่องจักร
     */
    private String machineCode;
}