package com.pharmaflow.producthealth.repositories;

import com.pharmaflow.producthealth.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByIsActiveTrue();

    List<Product> findByRequiresPrescription(Boolean requiresPrescription);

    // Pretraga po nazivu (case-insensitive)
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.isActive = true")
    List<Product> searchByName(@Param("keyword") String keyword);

    // Pronađi sve proizvode koji sadrže određenu supstancu
    @Query("SELECT p FROM Product p JOIN p.substances s WHERE s.id = :substanceId AND p.isActive = true")
    List<Product> findBySubstanceId(@Param("substanceId") Long substanceId);
}