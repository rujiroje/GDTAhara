package com.gdtahara.gdtaharabackend.util;

import com.gdtahara.gdtaharabackend.model.Machine;
import com.gdtahara.gdtaharabackend.model.Product;
import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.model.SubLot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class ZplLabelBuilderTest {

    private final ZplLabelBuilder builder = new ZplLabelBuilder();

    private SubLot buildStub(String subLotNumber, String productName, int qty, BigDecimal weight) {
        Machine machine = new Machine();
        machine.setMachineName("RBL101");

        Product product = new Product();
        product.setProductCode("P-BT500");
        product.setProductName(productName);

        ProductionReport report = new ProductionReport();
        report.setMachine(machine);
        report.setProduct(product);
        report.setShift("D");
        report.setStartDate(LocalDate.of(2025, 10, 1));

        SubLot subLot = new SubLot();
        subLot.setSubLotNumber(subLotNumber);
        subLot.setBoxQuantity(qty);
        subLot.setWeightKg(weight);
        subLot.setPalletNumber("PLT-01");
        subLot.setConfirmedAt(LocalDateTime.of(2025, 10, 1, 8, 0));
        subLot.setProductionReport(report);
        return subLot;
    }

    // ── structure ────────────────────────────────────────────────────

    @Test
    void buildLabel_startsWithXaEndsWithXz() {
        SubLot stub = buildStub("PL-RBL101-20251001-D-B0001", "Bottle 500ml", 600,
                BigDecimal.valueOf(12.5));
        String zpl = builder.buildSubLotLabel(stub);

        assertThat(zpl).startsWith("^XA");
        assertThat(zpl).endsWith("^XZ");
    }

    @Test
    void buildLabel_containsBarcode() {
        SubLot stub = buildStub("PL-RBL101-20251001-D-B0001", "Bottle 500ml", 600,
                BigDecimal.valueOf(12.5));
        String zpl = builder.buildSubLotLabel(stub);

        assertThat(zpl).contains("^BCN");
        assertThat(zpl).contains("PL-RBL101-20251001-D-B0001");
    }

    @Test
    void buildLabel_containsProductNameAndQty() {
        SubLot stub = buildStub("PL-RBL101-20251001-D-B0001", "Bottle 500ml", 600,
                BigDecimal.valueOf(12.5));
        String zpl = builder.buildSubLotLabel(stub);

        assertThat(zpl).contains("Bottle 500ml");
        assertThat(zpl).contains("600");
    }

    // ── null-safety ──────────────────────────────────────────────────

    @Test
    void nullProduct_doesNotThrow() {
        SubLot stub = buildStub("PL-RBL101-20251001-D-B0001", "Bottle 500ml", 600,
                BigDecimal.valueOf(12.5));
        stub.getProductionReport().setProduct(null);

        assertThatNoException().isThrownBy(() -> {
            String zpl = builder.buildSubLotLabel(stub);
            assertThat(zpl).startsWith("^XA");
            assertThat(zpl).endsWith("^XZ");
        });
    }

    // ── escaping ─────────────────────────────────────────────────────

    @Test
    void escapeSpecialChars_noRawCaretOrTildeInFieldData() {
        SubLot stub = buildStub("PL-RBL101-20251001-D-B0001", "A^B~C", 100,
                BigDecimal.valueOf(5.0));
        String zpl = builder.buildSubLotLabel(stub);

        // The raw "A^B" and "B~C" sequences must not appear — ^ and ~ were escaped
        assertThat(zpl).doesNotContain("A^B");
        assertThat(zpl).doesNotContain("B~C");
        // Escaped form should be present instead
        assertThat(zpl).contains("A_B_C");
    }
}
