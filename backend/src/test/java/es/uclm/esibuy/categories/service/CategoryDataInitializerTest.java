package es.uclm.esibuy.categories.service;

import java.util.ArrayList;
import java.util.List;

import es.uclm.esibuy.categories.model.Category;
import es.uclm.esibuy.categories.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryDataInitializerTest {

    private static final List<String> DEFAULT_CATEGORY_NAMES = List.of(
            "Electrónica", "Hogar", "Moda", "Deportes", "Libros", "Otros");

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryDataInitializer categoryDataInitializer;

    @Test
    void shouldCreateAllDefaultCategoriesWhenCollectionIsEmpty() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of());

        categoryDataInitializer.run();

        verify(categoryRepository).saveAll(argThat(categories -> namesOf(categories).equals(DEFAULT_CATEGORY_NAMES)));
    }

    @Test
    void shouldCreateOnlyMissingDefaultsAndPreserveExistingCategories() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of(
                new Category("existing-electronics", "  electrónica "),
                new Category("custom-category", "Artesanía")));

        categoryDataInitializer.run();

        List<String> missingDefaultNames = DEFAULT_CATEGORY_NAMES.subList(1, DEFAULT_CATEGORY_NAMES.size());
        verify(categoryRepository).saveAll(argThat(categories -> namesOf(categories).equals(missingDefaultNames)));
    }

    @Test
    void shouldNotWriteWhenEveryDefaultCategoryAlreadyExists() throws Exception {
        when(categoryRepository.findAll()).thenReturn(DEFAULT_CATEGORY_NAMES.stream()
                .map(Category::new)
                .toList());

        categoryDataInitializer.run();

        verify(categoryRepository, never()).saveAll(anyList());
    }

    private List<String> namesOf(Iterable<Category> categories) {
        var names = new ArrayList<String>();
        categories.forEach(category -> names.add(category.getName()));
        return names;
    }
}
