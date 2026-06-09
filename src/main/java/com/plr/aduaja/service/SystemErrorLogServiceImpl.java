package com.plr.aduaja.service;

import com.plr.aduaja.model.SystemErrorLog;
import com.plr.aduaja.model.SystemErrorLog.ErrorLevel;
import com.plr.aduaja.repository.SystemErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemErrorLogServiceImpl implements SystemErrorLogService {

    private static final Logger log = LoggerFactory.getLogger(SystemErrorLogServiceImpl.class);

    @Autowired
    private SystemErrorLogRepository systemErrorLogRepository;

    @Override
    @Transactional
    public SystemErrorLog logError(String sourceClass, String sourceMethod,
                                    ErrorLevel errorLevel, String message,
                                    String stackTrace, String additionalData,
                                    String userId, String reportId) {
        try {
            SystemErrorLog errorLog = SystemErrorLog.create(
                sourceClass, sourceMethod, errorLevel, message,
                stackTrace, additionalData, userId, reportId
            );
            SystemErrorLog saved = systemErrorLogRepository.save(errorLog);

            if (errorLevel == ErrorLevel.ERROR || errorLevel == ErrorLevel.FATAL) {
                log.error("[SYSTEM_ERROR] {}::{} - {} (userId={}, reportId={})",
                    sourceClass, sourceMethod, message, userId, reportId);
            } else if (errorLevel == ErrorLevel.WARN) {
                log.warn("[SYSTEM_ERROR] {}::{} - {}", sourceClass, sourceMethod, message);
            }

            return saved;
        } catch (Exception e) {
            log.error("Gagal menyimpan SystemErrorLog: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    @Transactional
    public SystemErrorLog logError(String sourceClass, String sourceMethod,
                                    String message, Exception exception) {
        String stackTrace = null;
        if (exception != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            stackTrace = sw.toString();
            if (stackTrace.length() > 5000) {
                stackTrace = stackTrace.substring(0, 5000);
            }
        }
        return logError(sourceClass, sourceMethod,
            exception != null ? ErrorLevel.ERROR : ErrorLevel.WARN,
            message, stackTrace, null, null, null);
    }

    @Override
    @Transactional
    public SystemErrorLog logError(String sourceClass, String sourceMethod,
                                    String message, Exception exception,
                                    String userId, String reportId) {
        String stackTrace = null;
        if (exception != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            stackTrace = sw.toString();
            if (stackTrace.length() > 5000) {
                stackTrace = stackTrace.substring(0, 5000);
            }
        }
        return logError(sourceClass, sourceMethod,
            exception != null ? ErrorLevel.ERROR : ErrorLevel.WARN,
            message, stackTrace, null, userId, reportId);
    }

    @Override
    public List<SystemErrorLog> getErrorsByLevel(ErrorLevel errorLevel) {
        return systemErrorLogRepository.findByErrorLevelOrderByCreatedAtDesc(errorLevel);
    }

    @Override
    public List<SystemErrorLog> getErrorsBySource(String sourceClass) {
        return systemErrorLogRepository.findBySourceClassOrderByCreatedAtDesc(sourceClass);
    }

    @Override
    public List<SystemErrorLog> getRecentErrors(LocalDateTime since) {
        return systemErrorLogRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since);
    }

    @Override
    public List<SystemErrorLog> getAllErrors() {
        return systemErrorLogRepository.findAll();
    }

    @Override
    public long countByLevel(ErrorLevel errorLevel) {
        return systemErrorLogRepository.countByErrorLevel(errorLevel);
    }
}
