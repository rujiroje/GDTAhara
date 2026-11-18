package com.gdtahara.gdtaharabackend.print;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development/test stub — logs the ZPL instead of sending it to real hardware.
 * Active by default (matchIfMissing=true) so the app starts without any printer config.
 */
@Component
@ConditionalOnProperty(name = "label.printer.mode", havingValue = "log", matchIfMissing = true)
public class LoggingLabelPrinterClient implements LabelPrinterClient {

    private static final Logger logger = LoggerFactory.getLogger(LoggingLabelPrinterClient.class);

    @Override
    public void print(String zpl, String printerTarget) {
        logger.info("LABEL PRINT (log mode) target={} zpl_length={}\n{}", printerTarget, zpl.length(), zpl);
    }
}
