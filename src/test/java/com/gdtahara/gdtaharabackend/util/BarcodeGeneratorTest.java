package com.gdtahara.gdtaharabackend.util;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.*;

class BarcodeGeneratorTest {

    private final BarcodeGenerator generator = new BarcodeGenerator();

    @Test
    void code128_roundTrip() throws Exception {
        String data = "PL-RBL101-20251001-D-B0001";
        byte[] bytes = generator.generateCode128Png(data, 600, 150);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = new MultiFormatReader().decode(bitmap);

        assertThat(result.getText()).isEqualTo(data);
    }

    @Test
    void pngMagicBytes() {
        byte[] bytes = generator.generateCode128Png("TEST", 400, 120);

        assertThat(bytes[0] & 0xFF).isEqualTo(0x89);
        assertThat(bytes[1] & 0xFF).isEqualTo(0x50); // P
        assertThat(bytes[2] & 0xFF).isEqualTo(0x4E); // N
        assertThat(bytes[3] & 0xFF).isEqualTo(0x47); // G
    }

    @Test
    void blankInput_throws() {
        assertThatThrownBy(() -> generator.generateCode128Png("", 400, 120))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void qr_roundTrip() throws Exception {
        String data = "PL-RBL101-20251001-D-B0001";
        byte[] bytes = generator.generateQrPng(data, 300);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = new MultiFormatReader().decode(bitmap);

        assertThat(result.getText()).isEqualTo(data);
    }
}
