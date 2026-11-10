package es.uclm.esibuy.categories.service;

import java.util.List;

import es.uclm.esibuy.categories.dto.CategoryRequest;
import es.uclm.esibuy.categories.model.Category;
import es.uclm.esibuy.categories.repository.CategoryRepository;
import es.uclm.esibuy.common.exception.CategoryIdNotFoundException;
import es.uclm.esibuy.common.exception.CategoryInUseException;
import es.uclm.esibuy.common.exception.DuplicateCategoryException;
import es.uclm.esibuy.products.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category create(CategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateCategoryException();
        }
        return categoryRepository.save(new Category(name));
    }

    public Category update(String id, CategoryRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow(CategoryIdNotFoundException::new);
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateCategoryException();
        }
        category.setName(name);
        return categoryRepository.save(category);
    }

    public void delete(String id) {
        Category category = categoryRepository.findById(id).orElseThrow(CategoryIdNotFoundException::new);
        if (productRepository.existsByCategoryIdsContaining(id)) {
            throw new CategoryInUseException();
        }
        categoryRepository.delete(category);
    }
}
