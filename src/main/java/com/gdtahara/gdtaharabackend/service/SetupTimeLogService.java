package com.gdtahara.gdtaharabackend.service;

import com.gdtahara.gdtaharabackend.dto.SetupActivityCodeDto;
import com.gdtahara.gdtaharabackend.dto.SetupTimeLogDto;
import com.gdtahara.gdtaharabackend.dto.SetupTimeLogRequest;
import com.gdtahara.gdtaharabackend.dto.CreateActivityCodeRequest;
import java.util.List;

public interface SetupTimeLogService {
    List<SetupActivityCodeDto> getActiveCodes();
    SetupActivityCodeDto createCode(CreateActivityCodeRequest req, String username);
    SetupActivityCodeDto toggleCodeActive(Long codeId, boolean active);

    List<SetupTimeLogDto> getTimeLogs(Long jobId);
    SetupTimeLogDto startLog(Long jobId, SetupTimeLogRequest req, String username);
    SetupTimeLogDto endLog(Long jobId, Long logId, String username);
    void deleteLog(Long jobId, Long logId, String username);
    boolean hasRunningLogs(Long jobId);
    List<SetupTimeLogDto> endAllRunningLogs(Long jobId, String username);
}
