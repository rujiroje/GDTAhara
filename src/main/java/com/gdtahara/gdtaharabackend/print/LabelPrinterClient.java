package com.gdtahara.gdtaharabackend.print;

public interface LabelPrinterClient {

    /**
     * Sends {@code zpl} to the printer identified by {@code printerTarget}.
     * For TCP mode the target format is {@code "host:port"} (default port 9100).
     */
    void print(String zpl, String printerTarget) throws PrintException;
}
