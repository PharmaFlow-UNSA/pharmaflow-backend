package com.pharmaflow.producthealth.services;

import com.pharmaflow.producthealth.models.Category;
import com.pharmaflow.producthealth.models.Product;
import com.pharmaflow.producthealth.models.Substance;
import com.pharmaflow.producthealth.repositories.CategoryRepository;
import com.pharmaflow.producthealth.repositories.ProductRepository;
import com.pharmaflow.producthealth.repositories.SubstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubstanceRepository substanceRepository;

    public List<Product> getAllActiveProducts() {
        return productRepository.findByIsActiveTrue();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proizvod sa ID " + id + " nije pronađen."));
    }

    public List<Product> searchProducts(String keyword) {
        return productRepository.searchByName(keyword);
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public List<Product> getProductsBySubstance(Long substanceId) {
        return productRepository.findBySubstanceId(substanceId);
    }

    @Transactional
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = getProductById(id);
        product.setIsActive(false);
        productRepository.save(product);
    }
}