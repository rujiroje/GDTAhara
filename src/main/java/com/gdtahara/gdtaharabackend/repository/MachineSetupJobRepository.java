package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.MachineSetupJob;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * spring.jpa.open-in-view=false → session closes when the @Transactional service method returns.
 * All queries that return entities to the controller MUST eagerly fetch the associations used in
 * SetupJobResponse::from (machine, fromProduct, toProduct, assignedTo, productionPlan, completedBy).
 * @EntityGraph forces LEFT JOIN FETCH on each named attribute — no lazy proxies reach the controller.
 */
@Repository
public interface MachineSetupJobRepository extends JpaRepository<MachineSetupJob, Long> {

    // @EntityGraph on findById so assign/start/complete/skip service methods also work.
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"machine", "fromProduct", "toProduct",
                                   "assignedTo", "productionPlan", "completedBy"})
    Optional<MachineSetupJob> findById(@NonNull Long id);

    @EntityGraph(attributePaths = {"machine", "fromProduct", "toProduct",
                                   "assignedTo", "productionPlan", "completedBy"})
    List<MachineSetupJob> findByMachineIdAndPlanDate(Long machineId, LocalDate planDate);

    @EntityGraph(attributePaths = {"machine", "fromProduct", "toProduct",
                                   "assignedTo", "productionPlan", "completedBy"})
    List<MachineSetupJob> findByAssignedToIdAndStatusIn(Long userId, List<String> statuses);

    @EntityGraph(attributePaths = {"machine", "fromProduct", "toProduct",
                                   "assignedTo", "productionPlan", "completedBy"})
    Optional<MachineSetupJob> findFirstByProductionPlanIdAndStatusIn(Long planId, List<String> statuses);

    /** Unassigned jobs — merged with assigned-to-user in service to build the technician queue. */
    @EntityGraph(attributePaths = {"machine", "fromProduct", "toProduct",
                                   "assignedTo", "productionPlan", "completedBy"})
    List<MachineSetupJob> findByAssignedToIsNullAndStatusInOrderByPlanDateAsc(List<String> statuses);

    /** All active setup jobs — PC/Admin dashboard. */
    @EntityGraph(attributePaths = {"machine", "fromProduct", "toProduct",
                                   "assignedTo", "productionPlan", "completedBy"})
    List<MachineSetupJob> findByStatusInOrderByPlanDateAsc(List<String> statuses);
}
