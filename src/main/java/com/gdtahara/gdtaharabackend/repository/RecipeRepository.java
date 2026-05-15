package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    Optional<Recipe> findByRecipeCode(String recipeCode);

    List<Recipe> findByIsActiveTrue();

    List<Recipe> findByProductIdAndIsActiveTrue(Long productId);

    @Query("SELECT r FROM Recipe r WHERE r.isActive = true " +
           "AND (:machineId IS NULL OR r.machine.id = :machineId OR r.machine IS NULL) " +
           "AND r.product.id = :productId")
    List<Recipe> findActiveByProductAndMachine(@Param("productId") Long productId,
                                               @Param("machineId") Long machineId);
}
