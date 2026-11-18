package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.SetupTimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SetupTimeLogRepository extends JpaRepository<SetupTimeLog, Long> {

    @Query("""
            SELECT l FROM SetupTimeLog l
            JOIN FETCH l.activityCode
            LEFT JOIN FETCH l.createdBy
            WHERE l.setupJob.id = :jobId
            ORDER BY l.startTime ASC
            """)
    List<SetupTimeLog> findByJobIdWithDetails(@Param("jobId") Long jobId);

    @Query("SELECT COUNT(l) > 0 FROM SetupTimeLog l WHERE l.setupJob.id = :jobId AND l.endTime IS NULL")
    boolean existsRunningByJobId(@Param("jobId") Long jobId);

    @Query("""
            SELECT l FROM SetupTimeLog l
            JOIN FETCH l.activityCode
            WHERE l.setupJob.id = :jobId AND l.endTime IS NULL
            """)
    List<SetupTimeLog> findRunningByJobId(@Param("jobId") Long jobId);

    @Query("SELECT COALESCE(MAX(l.sequenceNo), 0) FROM SetupTimeLog l WHERE l.setupJob.id = :jobId")
    int maxSequenceNoByJobId(@Param("jobId") Long jobId);
}
