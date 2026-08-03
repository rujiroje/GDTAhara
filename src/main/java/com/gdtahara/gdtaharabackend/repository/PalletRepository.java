package com.gdtahara.gdtaharabackend.repository;

import com.gdtahara.gdtaharabackend.model.Pallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PalletRepository extends JpaRepository<Pallet, Long> {

    @Query("SELECT p FROM Pallet p LEFT JOIN FETCH p.product LEFT JOIN FETCH p.createdBy WHERE p.status = 'OPEN' ORDER BY p.createdAt DESC")
    List<Pallet> findAllOpen();

    @Query("SELECT p FROM Pallet p LEFT JOIN FETCH p.product LEFT JOIN FETCH p.createdBy WHERE p.status = 'OPEN' AND p.createdBy.username = :username ORDER BY p.createdAt DESC")
    List<Pallet> findOpenByUsername(@Param("username") String username);

    @Query("SELECT p FROM Pallet p LEFT JOIN FETCH p.product WHERE p.palletDate = :date ORDER BY p.palletNumber ASC")
    List<Pallet> findByPalletDate(@Param("date") LocalDate date);

    @Query("SELECT p FROM Pallet p LEFT JOIN FETCH p.product WHERE p.product.id = :productId AND p.palletDate = :date ORDER BY p.palletNumber ASC")
    List<Pallet> findByProductAndDate(@Param("productId") Long productId, @Param("date") LocalDate date);

    @Query("SELECT p FROM Pallet p LEFT JOIN FETCH p.product WHERE p.parentPallet.id = :parentId ORDER BY p.createdAt ASC")
    List<Pallet> findDerivedFrom(@Param("parentId") Long parentId);

    Optional<Pallet> findByPalletNumberAndPalletDateAndProduct_IdAndRevisionSuffix(
            String palletNumber, LocalDate palletDate, Long productId, String revisionSuffix);

    @Query("SELECT p FROM Pallet p LEFT JOIN FETCH p.product LEFT JOIN FETCH p.createdBy WHERE p.id = :id")
    Optional<Pallet> findByIdWithDetails(@Param("id") Long id);
}
