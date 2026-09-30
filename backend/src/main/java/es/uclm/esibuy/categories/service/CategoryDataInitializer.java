package es.uclm.esibuy.categories.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import es.uclm.esibuy.categories.model.Category;
import es.uclm.esibuy.categories.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CategoryDataInitializer implements CommandLineRunner {

    private static final List<String> DEFAULT_CATEGORY_NAMES = List.of(
            "Electrónica", "Hogar", "Moda", "Deportes", "Libros", "Otros");

    private final CategoryRepository categoryRepository;

    public CategoryDataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        Set<String> existingNames = new HashSet<>();
        categoryRepository.findAll().stream()
                .map(Category::getNombre)
                .filter(name -> name != null && !name.isBlank())
                .map(CategoryDataInitializer::normalizeName)
                .forEach(existingNames::add);

        List<Category> missingCategories = DEFAULT_CATEGORY_NAMES.stream()
                .filter(name -> !existingNames.contains(normalizeName(name)))
                .map(Category::new)
                .toList();

        if (!missingCategories.isEmpty()) {
            categoryRepository.saveAll(missingCategories);
        }
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
