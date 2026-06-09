package com.plr.aduaja.repository;

import com.plr.aduaja.model.SystemErrorLog;
import com.plr.aduaja.model.SystemErrorLog.ErrorLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemErrorLogRepository extends JpaRepository<SystemErrorLog, String> {

    List<SystemErrorLog> findByErrorLevelOrderByCreatedAtDesc(ErrorLevel errorLevel);

    List<SystemErrorLog> findBySourceClassOrderByCreatedAtDesc(String sourceClass);

    List<SystemErrorLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after);

    List<SystemErrorLog> findByErrorLevelAndCreatedAtAfterOrderByCreatedAtDesc(ErrorLevel errorLevel, LocalDateTime after);

    long countByErrorLevel(ErrorLevel errorLevel);
}
