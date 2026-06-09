package com.plr.aduaja.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_error_logs")
public class SystemErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "error_id")
    private String errorId;

    @Column(name = "source_class", length = 255)
    private String sourceClass;

    @Column(name = "source_method", length = 255)
    private String sourceMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_level", nullable = false, length = 20)
    private ErrorLevel errorLevel = ErrorLevel.ERROR;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData;

    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "report_id", length = 255)
    private String reportId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ErrorLevel {
        INFO, WARN, ERROR, FATAL
    }

    public String getErrorId() { return errorId; }
    public void setErrorId(String errorId) { this.errorId = errorId; }

    public String getSourceClass() { return sourceClass; }
    public void setSourceClass(String sourceClass) { this.sourceClass = sourceClass; }

    public String getSourceMethod() { return sourceMethod; }
    public void setSourceMethod(String sourceMethod) { this.sourceMethod = sourceMethod; }

    public ErrorLevel getErrorLevel() { return errorLevel; }
    public void setErrorLevel(ErrorLevel errorLevel) { this.errorLevel = errorLevel; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public String getAdditionalData() { return additionalData; }
    public void setAdditionalData(String additionalData) { this.additionalData = additionalData; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static SystemErrorLog create(String sourceClass, String sourceMethod,
                                         ErrorLevel errorLevel, String message,
                                         String stackTrace, String additionalData,
                                         String userId, String reportId) {
        SystemErrorLog log = new SystemErrorLog();
        log.setSourceClass(sourceClass);
        log.setSourceMethod(sourceMethod);
        log.setErrorLevel(errorLevel);
        log.setMessage(message);
        log.setStackTrace(stackTrace);
        log.setAdditionalData(additionalData);
        log.setUserId(userId);
        log.setReportId(reportId);
        return log;
    }
}
