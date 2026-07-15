package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.SetupJobStepResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SetupJobStepResultRepository extends JpaRepository<SetupJobStepResult, Long> {

    List<SetupJobStepResult> findBySetupJobIdOrderByTemplateStepOrderAsc(Long setupJobId);

    Optional<SetupJobStepResult> findBySetupJobIdAndTemplateId(Long setupJobId, Long templateId);
}
