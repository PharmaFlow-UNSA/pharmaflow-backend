package com.pharmaflow.producthealth.repositories;

import com.pharmaflow.producthealth.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @EntityGraph(attributePaths = {"category", "substances"})
    Optional<Product> findById(Long id);

    Optional<Product> findByBarcode(String barcode);
    boolean existsByBarcode(String barcode);

    // ── JOIN FETCH queries (N+1 safe) ────────────────────────────────────────

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

    // ── Paginacija i sortiranje ───────────────────────────────────────────────

    @Query(value = "SELECT DISTINCT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.substances WHERE p.isActive = true",
           countQuery = "SELECT COUNT(DISTINCT p) FROM Product p WHERE p.isActive = true")
    Page<Product> findAllActivePageable(Pageable pageable);

    @Query(value = "SELECT DISTINCT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.substances " +
                   "WHERE p.category.id = :categoryId AND p.isActive = true",
           countQuery = "SELECT COUNT(DISTINCT p) FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true")
    Page<Product> findByCategoryIdPageable(@Param("categoryId") Long categoryId, Pageable pageable);

    // ── Custom upiti ─────────────────────────────────────────────────────────

    // Pretraga po opsegu cijena
    @Query("SELECT DISTINCT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.substances " +
           "WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.isActive = true")
    List<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    // Pretraga po tipu i dostupnosti recepta
    @Query("SELECT DISTINCT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.substances " +
           "WHERE p.productType = :type AND p.requiresPrescription = :requiresPrescription AND p.isActive = true")
    List<Product> findByTypeAndPrescription(
            @Param("type") Product.ProductType type,
            @Param("requiresPrescription") Boolean requiresPrescription);

    // Proizvodi bez recepta po kategoriji — za OTC prikaz
    @Query("SELECT DISTINCT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.substances " +
           "WHERE p.requiresPrescription = false AND p.isActive = true " +
           "ORDER BY p.price ASC")
    List<Product> findAllOtcOrderByPriceAsc();

    // Statistika — broj proizvoda po tipu
    @Query("SELECT p.productType, COUNT(p) FROM Product p WHERE p.isActive = true GROUP BY p.productType")
    List<Object[]> countByProductType();

    // Batch update kategorije za listu proizvoda — dio transakcijske metode
    @Modifying
    @Query("UPDATE Product p SET p.category.id = :newCategoryId WHERE p.id IN :productIds")
    int bulkUpdateCategory(@Param("productIds") List<Long> productIds, @Param("newCategoryId") Long newCategoryId);

    // Legacy methods kept for compatibility
    List<Product> findByIsActiveTrue();
    List<Product> findByCategoryId(Long categoryId);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.isActive = true")
    List<Product> searchByName(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p JOIN p.substances s WHERE s.id = :substanceId AND p.isActive = true")
    List<Product> findBySubstanceId(@Param("substanceId") Long substanceId);
}
