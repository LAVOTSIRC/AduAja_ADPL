package com.plr.aduaja.service;

import com.plr.aduaja.model.SystemErrorLog;
import com.plr.aduaja.model.SystemErrorLog.ErrorLevel;

import java.time.LocalDateTime;
import java.util.List;

public interface SystemErrorLogService {

    SystemErrorLog logError(String sourceClass, String sourceMethod,
                            ErrorLevel errorLevel, String message,
                            String stackTrace, String additionalData,
                            String userId, String reportId);

    SystemErrorLog logError(String sourceClass, String sourceMethod,
                            String message, Exception exception);

    SystemErrorLog logError(String sourceClass, String sourceMethod,
                            String message, Exception exception,
                            String userId, String reportId);

    List<SystemErrorLog> getErrorsByLevel(ErrorLevel errorLevel);

    List<SystemErrorLog> getErrorsBySource(String sourceClass);

    List<SystemErrorLog> getRecentErrors(LocalDateTime since);

    List<SystemErrorLog> getAllErrors();

    long countByLevel(ErrorLevel errorLevel);
}
