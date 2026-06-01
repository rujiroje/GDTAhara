package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.model.SubLot;
import com.gdtahara.gdtaharabackend.print.LabelPrinterClient;
import com.gdtahara.gdtaharabackend.util.ZplLabelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class LabelPrintService {

    private static final Logger logger = LoggerFactory.getLogger(LabelPrintService.class);
    private static final DateTimeFormatter REF_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final SubLotService      subLotService;
    private final ZplLabelBuilder    zplLabelBuilder;
    private final LabelPrinterClient printerClient;

    public LabelPrintService(SubLotService subLotService,
                             ZplLabelBuilder zplLabelBuilder,
                             LabelPrinterClient printerClient) {
        this.subLotService   = subLotService;
        this.zplLabelBuilder = zplLabelBuilder;
        this.printerClient   = printerClient;
    }

    /**
     * Returns the ZPL string for the given sub-lot without printing it.
     * The read-only transaction keeps the JPA session open so lazy associations
     * (productionReport → product/machine) are resolved inside ZplLabelBuilder.
     */
    @Transactional(readOnly = true)
    public String previewSubLotLabel(Long subLotId) {
        SubLot subLot = subLotService.getById(subLotId);
        logger.debug("Preview ZPL for sub-lot {}", subLotId);
        return zplLabelBuilder.buildSubLotLabel(subLot);
    }

    /**
     * Builds the ZPL, sends it to the printer, then marks the sub-lot as labeled.
     * TCP I/O is intentionally outside the DB transaction (markAsLabeled has its own).
     */
    public void printSubLotLabel(Long subLotId, String printerTarget, String username) {
        logger.info("Print label request: subLotId={} target={} by={}", subLotId, printerTarget, username);

        SubLot subLot = subLotService.getById(subLotId);
        String zpl = zplLabelBuilder.buildSubLotLabel(subLot);

        printerClient.print(zpl, printerTarget);

        String ref = printerTarget + "@" + LocalDateTime.now().format(REF_FMT);
        subLotService.markAsLabeled(subLotId, ref, username);
        logger.info("Sub-lot {} labeled via {}", subLotId, printerTarget);
    }
}
