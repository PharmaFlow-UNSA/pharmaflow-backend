package com.pharmaflow.producthealth.services;

import com.pharmaflow.producthealth.dto.CategoryDTO;
import com.pharmaflow.producthealth.exception.DuplicateResourceException;
import com.pharmaflow.producthealth.exception.ResourceNotFoundException;
import com.pharmaflow.producthealth.models.Category;
import com.pharmaflow.producthealth.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream().map(c -> modelMapper.map(c, CategoryDTO.class)).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategorija sa ID " + id + " nije pronađena."));
        return modelMapper.map(category, CategoryDTO.class);
    }

    @Transactional
    public CategoryDTO createCategory(CategoryDTO dto) {
        if (categoryRepository.existsByName(dto.getName()))
            throw new DuplicateResourceException("Kategorija sa nazivom '" + dto.getName() + "' već postoji.");
        Category category = modelMapper.map(dto, Category.class);
        category.setId(null);
        return modelMapper.map(categoryRepository.save(category), CategoryDTO.class);
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategorija sa ID " + id + " nije pronađena."));
        if (!category.getName().equals(dto.getName()) && categoryRepository.existsByName(dto.getName()))
            throw new DuplicateResourceException("Kategorija sa nazivom '" + dto.getName() + "' već postoji.");
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setParentCategoryId(dto.getParentCategoryId());
        return modelMapper.map(categoryRepository.save(category), CategoryDTO.class);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id))
            throw new ResourceNotFoundException("Kategorija sa ID " + id + " nije pronađena.");
        categoryRepository.deleteById(id);
    }
}
