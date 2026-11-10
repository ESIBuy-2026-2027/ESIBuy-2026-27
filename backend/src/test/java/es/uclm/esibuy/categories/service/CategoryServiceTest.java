package es.uclm.esibuy.categories.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import es.uclm.esibuy.categories.dto.CategoryRequest;
import es.uclm.esibuy.categories.model.Category;
import es.uclm.esibuy.categories.repository.CategoryRepository;
import es.uclm.esibuy.common.exception.CategoryIdNotFoundException;
import es.uclm.esibuy.common.exception.CategoryInUseException;
import es.uclm.esibuy.common.exception.DuplicateCategoryException;
import es.uclm.esibuy.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, productRepository);
    }

    @Test
    void createsCategoryWithTrimmedName() {
        CategoryRequest request = request("  Papelería  ");
        when(categoryRepository.existsByNameIgnoreCase("Papelería")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        Category result = service.create(request);

        assertEquals("Papelería", result.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void rejectsDuplicateNameIgnoringCaseAndSurroundingSpaces() {
        CategoryRequest request = request("  eLeCtRóNiCa  ");
        when(categoryRepository.existsByNameIgnoreCase("eLeCtRóNiCa")).thenReturn(true);

        assertThrows(DuplicateCategoryException.class, () -> service.create(request));

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updatesExistingCategoryWithoutTreatingItsOwnNameAsDuplicate() {
        Category category = new Category("category-1", "Electrónica");
        when(categoryRepository.findById("category-1")).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Electrónica", "category-1")).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        Category result = service.update("category-1", request(" Electrónica "));

        assertSame(category, result);
        assertEquals("Electrónica", category.getName());
        verify(categoryRepository).save(category);
    }

    @Test
    void rejectsRenameToAnExistingCategoryName() {
        Category category = new Category("category-1", "Moda");
        when(categoryRepository.findById("category-1")).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("hogar", "category-1")).thenReturn(true);

        assertThrows(DuplicateCategoryException.class, () -> service.update("category-1", request("hogar")));

        assertEquals("Moda", category.getName());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deletesCategoryWhenNoProductUsesIt() {
        Category category = new Category("category-1", "Moda");
        when(categoryRepository.findById("category-1")).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryIdsContaining("category-1")).thenReturn(false);

        service.delete("category-1");

        verify(categoryRepository).delete(category);
    }

    @Test
    void rejectsDeletingCategoryUsedByAProduct() {
        Category category = new Category("category-1", "Moda");
        when(categoryRepository.findById("category-1")).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryIdsContaining("category-1")).thenReturn(true);

        assertThrows(CategoryInUseException.class, () -> service.delete("category-1"));

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void rejectsUpdatesAndDeletesForUnknownIds() {
        when(categoryRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(CategoryIdNotFoundException.class, () -> service.update("missing", request("Hogar")));
        assertThrows(CategoryIdNotFoundException.class, () -> service.delete("missing"));

        verify(productRepository, never()).existsByCategoryIdsContaining("missing");
    }

    private CategoryRequest request(String name) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);
        return request;
    }
}
