package com.gdtahara.gdtaharabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "low_stock_alert")
public class LowStockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_code", nullable = false)
    private String materialCode;

    @Column(name = "balance_at_alert", nullable = false)
    private BigDecimal balanceAtAlert;

    @Column(name = "reorder_point", nullable = false)
    private BigDecimal reorderPoint;

    @Column(name = "est_days_remaining")
    private BigDecimal estDaysRemaining;

    @Column(name = "alert_date", nullable = false)
    private LocalDate alertDate;

    @Column(name = "telegram_sent")
    private Boolean telegramSent;

    @Column(name = "telegram_sent_at")
    private LocalDateTime telegramSentAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
