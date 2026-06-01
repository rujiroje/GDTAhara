package com.gdtahara.gdtaharabackend.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@Component
public class BarcodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(BarcodeGenerator.class);
    private static final int MAX_DATA_LENGTH = 100;

    /**
     * Encodes {@code data} as a Code128 barcode and returns the result as a PNG byte array.
     * Margin is set to 10 pixels of quiet zone to ensure scanner / decoder compatibility.
     */
    public byte[] generateCode128Png(String data, int width, int height) {
        validateData(data);
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 10);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = new Code128Writer()
                    .encode(data, BarcodeFormat.CODE_128, width, height, hints);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            logger.debug("Code128 PNG generated: {} bytes for data length {}", baos.size(), data.length());
            return baos.toByteArray();
        } catch (IOException e) {
            logger.error("Code128 generation failed for '{}': {}", data, e.getMessage());
            throw new RuntimeException("Code128 generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Encodes {@code data} as a QR code and returns a square PNG of {@code size}×{@code size} pixels.
     */
    public byte[] generateQrPng(String data, int size) {
        validateData(data);
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 4);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

            BitMatrix matrix = new QRCodeWriter()
                    .encode(data, BarcodeFormat.QR_CODE, size, size, hints);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            logger.debug("QR PNG generated: {} bytes for data length {}", baos.size(), data.length());
            return baos.toByteArray();
        } catch (WriterException | IOException e) {
            logger.error("QR generation failed for '{}': {}", data, e.getMessage());
            throw new RuntimeException("QR generation failed: " + e.getMessage(), e);
        }
    }

    private void validateData(String data) {
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("Barcode data must not be blank");
        }
        if (data.length() > MAX_DATA_LENGTH) {
            throw new IllegalArgumentException(
                    "Barcode data exceeds maximum length of " + MAX_DATA_LENGTH + " characters");
        }
    }
}
