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
    boolean existsByBarcode(String barcode);

    // JOIN FETCH category + substances - eliminira N+1 problem
    @Query("SELECT DISTINCT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.substances WHERE p.isActive = true")
    List<Product> findAllActiveWithDetails();

    @Query("SELECT DISTINCT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.substances " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.isActive = true")
    List<Product> searchByNameWithDetails(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.substances " +
           "WHERE p.category.id = :categoryId AND p.isActive = true")
    List<Product> findByCategoryIdWithDetails(@Param("categoryId") Long categoryId);

    @Query("SELECT DISTINCT p FROM Product p JOIN FETCH p.category JOIN p.substances s " +
           "LEFT JOIN FETCH p.substances WHERE s.id = :substanceId AND p.isActive = true")
    List<Product> findBySubstanceIdWithDetails(@Param("substanceId") Long substanceId);

    // Stare metode - zadržane za kompatibilnost
    List<Product> findByIsActiveTrue();
    List<Product> findByCategoryId(Long categoryId);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.isActive = true")
    List<Product> searchByName(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p JOIN p.substances s WHERE s.id = :substanceId AND p.isActive = true")
    List<Product> findBySubstanceId(@Param("substanceId") Long substanceId);
}
