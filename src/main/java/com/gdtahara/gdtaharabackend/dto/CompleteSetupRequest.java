package com.gdtahara.gdtaharabackend.dto;

import lombok.Data;

@Data
public class CompleteSetupRequest {
    private boolean moldChanged;
    private String moldCodeFrom;
    private String moldCodeTo;
    private boolean tempAdjusted;
    private boolean cycleAdjusted;
    private boolean blowPinAligned;
    private boolean fpiPassed;
    private String notes;
}
