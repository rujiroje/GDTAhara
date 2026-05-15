package com.gdtahara.gdtaharabackend;

/**
 * Dedicated launcher to disambiguate the main class in a multi-root workspace.
 * Use this class in VS Code launch configurations to run the backend reliably.
 */
public class DevLauncher {
    public static void main(String[] args) {
        GdtaharaBackendApplication.main(args);
    }
}
