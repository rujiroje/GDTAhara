package com.gdtahara.gdtaharabackend.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SerialNumberUtil {

    private static final Pattern SUB_LOT_PATTERN =
            Pattern.compile("^PL-[A-Za-z0-9]+-\\d{8}-[DN]-B\\d{4}$");

    public boolean isValidSubLotNumber(String s) {
        if (s == null || s.isBlank()) return false;
        return SUB_LOT_PATTERN.matcher(s).matches();
    }

    /**
     * Luhn Mod-10 check digit for a string of decimal digits.
     * "7992739871" → 3  (standard Luhn reference vector)
     */
    public int mod10CheckDigit(String numericPart) {
        if (numericPart == null || numericPart.isEmpty()) {
            throw new IllegalArgumentException("numericPart must not be null or empty");
        }
        for (char c : numericPart.toCharArray()) {
            if (!Character.isDigit(c)) {
                throw new IllegalArgumentException(
                        "numericPart must contain only digits, found: '" + c + "'");
            }
        }
        int sum = 0;
        int len = numericPart.length();
        for (int i = 0; i < len; i++) {
            int digit = numericPart.charAt(len - 1 - i) - '0';
            if (i % 2 == 0) {   // positions 1,3,5,… from right (will become even after appending check digit)
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
        }
        return (10 - (sum % 10)) % 10;
    }

    public String compactSerial(long globalSeq) {
        return String.format("%010d", globalSeq);
    }
}
