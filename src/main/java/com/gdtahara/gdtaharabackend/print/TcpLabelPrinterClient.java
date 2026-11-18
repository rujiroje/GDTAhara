package com.gdtahara.gdtaharabackend.print;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Sends ZPL over a raw TCP socket to a Zebra/TSC printer (JetDirect, port 9100).
 * Activate with: label.printer.mode=tcp
 * Intended for W18 when real hardware is available; wired up now so the switch is trivial.
 */
@Component
@ConditionalOnProperty(name = "label.printer.mode", havingValue = "tcp")
public class TcpLabelPrinterClient implements LabelPrinterClient {

    private static final Logger logger = LoggerFactory.getLogger(TcpLabelPrinterClient.class);
    private static final int DEFAULT_PORT = 9100;
    private static final int TIMEOUT_MS   = 5_000;

    @Override
    public void print(String zpl, String printerTarget) throws PrintException {
        String[] parts = printerTarget.split(":", 2);
        String host = parts[0];
        int    port = parts.length > 1 ? Integer.parseInt(parts[1]) : DEFAULT_PORT;

        logger.info("Sending ZPL ({} chars) to {}:{}", zpl.length(), host, port);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            socket.getOutputStream().write(zpl.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            logger.info("ZPL sent successfully to {}:{}", host, port);
        } catch (IOException e) {
            logger.error("TCP print failed for {}:{}: {}", host, port, e.getMessage());
            throw new PrintException("TCP print failed [" + host + ":" + port + "]: " + e.getMessage(), e);
        }
    }
}
