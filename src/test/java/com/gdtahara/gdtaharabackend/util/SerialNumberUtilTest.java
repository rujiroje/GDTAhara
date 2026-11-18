package com.gdtahara.gdtaharabackend.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SerialNumberUtilTest {

    private final SerialNumberUtil util = new SerialNumberUtil();

    // ── isValidSubLotNumber ──────────────────────────────────────────────────

    @Test
    void isValid_valid() {
        assertThat(util.isValidSubLotNumber("PL-RBL101-20251001-D-B0001")).isTrue();
        assertThat(util.isValidSubLotNumber("PL-RBL101-20251001-N-B9999")).isTrue();
    }

    @Test
    void isValid_shortDate_false() {
        assertThat(util.isValidSubLotNumber("PL-RBL101-2025-D-B1")).isFalse();
    }

    @Test
    void isValid_nullOrBlank_false() {
        assertThat(util.isValidSubLotNumber(null)).isFalse();
        assertThat(util.isValidSubLotNumber("")).isFalse();
    }

    @Test
    void isValid_unknownShift_false() {
        assertThat(util.isValidSubLotNumber("PL-RBL101-20251001-X-B0001")).isFalse();
    }

    // ── mod10CheckDigit ──────────────────────────────────────────────────────

    @Test
    void mod10_knownResult() {
        // Standard Luhn reference: "7992739871" → check digit 3
        assertThat(util.mod10CheckDigit("7992739871")).isEqualTo(3);
    }

    @Test
    void mod10_deterministic() {
        int first  = util.mod10CheckDigit("7992739871");
        int second = util.mod10CheckDigit("7992739871");
        assertThat(first).isEqualTo(second);
    }

    @Test
    void mod10_nonDigit_throws() {
        assertThatThrownBy(() -> util.mod10CheckDigit("123A56"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── compactSerial ────────────────────────────────────────────────────────

    @Test
    void compactSerial_zeroPad() {
        assertThat(util.compactSerial(42L)).isEqualTo("0000000042");
        assertThat(util.compactSerial(0L)).isEqualTo("0000000000");
        assertThat(util.compactSerial(9_999_999_999L)).isEqualTo("9999999999");
    }
}
