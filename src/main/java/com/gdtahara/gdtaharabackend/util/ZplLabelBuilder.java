package com.gdtahara.gdtaharabackend.util;

import com.gdtahara.gdtaharabackend.model.Machine;
import com.gdtahara.gdtaharabackend.model.Product;
import com.gdtahara.gdtaharabackend.model.ProductionReport;
import com.gdtahara.gdtaharabackend.model.SubLot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class ZplLabelBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ZplLabelBuilder.class);
    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Builds a complete ZPL string for a 4×6-inch (203dpi) sub-lot label.
     * Includes a Code128 barcode via the ZPL ^BC command — no image bytes required.
     */
    public String buildSubLotLabel(SubLot subLot) {
        if (subLot == null) throw new IllegalArgumentException("subLot must not be null");

        ProductionReport report = subLot.getProductionReport();
        Product  product = (report != null) ? report.getProduct()  : null;
        Machine  machine = (report != null) ? report.getMachine()  : null;

        String subLotNumber = safe(subLot.getSubLotNumber());
        String productName  = escape(product != null ? product.getProductName() : null);
        String productCode  = escape(product != null ? product.getProductCode()  : null);
        String machineName  = escape(machine != null ? machine.getMachineName() : null);
        String shift        = escape(report  != null ? report.getShift()         : null);
        String startDate    = (report != null && report.getStartDate() != null)
                              ? report.getStartDate().format(DATE_FMT) : "-";
        String qty          = subLot.getBoxQuantity() != null
                              ? String.valueOf(subLot.getBoxQuantity()) : "-";
        String weight       = subLot.getWeightKg() != null
                              ? subLot.getWeightKg().toPlainString() : "-";
        String pallet       = escape(subLot.getPalletNumber());
        String confirmedAt  = subLot.getConfirmedAt() != null
                              ? subLot.getConfirmedAt().format(DATETIME_FMT) : "-";

        logger.debug("Building ZPL label for sub-lot: {}", subLotNumber);

        StringBuilder z = new StringBuilder(600);
        z.append("^XA\n");
        z.append("^PW812\n");   // 4 in × 203 dpi
        z.append("^LL1218\n");  // 6 in × 203 dpi

        // ── Product header ────────────────────────────────────────────
        z.append("^FO30,20^A0N,32,24^FD").append(productName)
         .append("  [").append(productCode).append("]^FS\n");

        // ── Sub-lot number (large) ────────────────────────────────────
        z.append("^FO30,65^A0N,48,36^FD").append(subLotNumber).append("^FS\n");

        // ── Code128 barcode (ZPL native, no image) ───────────────────
        z.append("^FO30,125\n");
        z.append("^BY2\n");
        z.append("^BCN,100,Y,N,N\n");
        z.append("^FD").append(subLotNumber).append("^FS\n");

        // ── Separator ────────────────────────────────────────────────
        z.append("^FO30,285^GB752,3,3^FS\n");

        // ── Qty / Weight / Pallet ─────────────────────────────────────
        z.append("^FO30,295^A0N,30,22^FDQty:^FS^FO180,295^A0N,30,22^FD").append(qty).append(" pcs^FS\n");
        z.append("^FO30,335^A0N,30,22^FDWeight:^FS^FO180,335^A0N,30,22^FD").append(weight).append(" kg^FS\n");
        z.append("^FO30,375^A0N,30,22^FDPallet:^FS^FO180,375^A0N,30,22^FD").append(pallet).append("^FS\n");

        // ── Separator ────────────────────────────────────────────────
        z.append("^FO30,415^GB752,3,3^FS\n");

        // ── Machine / Shift / Date ────────────────────────────────────
        z.append("^FO30,425^A0N,28,20^FDMachine:^FS^FO190,425^A0N,28,20^FD").append(machineName).append("^FS\n");
        z.append("^FO30,460^A0N,28,20^FDShift:^FS^FO190,460^A0N,28,20^FD").append(shift).append("^FS\n");
        z.append("^FO30,495^A0N,28,20^FDDate:^FS^FO190,495^A0N,28,20^FD").append(startDate).append("^FS\n");

        // ── Confirmed timestamp ───────────────────────────────────────
        z.append("^FO30,535^A0N,25,18^FDConfirmed: ").append(confirmedAt).append("^FS\n");

        z.append("^XZ");

        logger.info("ZPL label built for sub-lot {}: {} chars", subLotNumber, z.length());
        return z.toString();
    }

    /**
     * Replaces ZPL special characters (^, ~, \) in user-supplied text so they
     * are not misinterpreted as ZPL commands inside ^FD...^FS field data.
     */
    private String escape(String s) {
        if (s == null) return "-";
        return s.replace("\\", "_").replace("^", "_").replace("~", "_");
    }

    private String safe(String s) {
        return s != null ? s : "-";
    }
}
