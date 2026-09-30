package es.uclm.esibuy.categories.controller;

import java.util.List;

import es.uclm.esibuy.categories.model.Category;
import es.uclm.esibuy.categories.service.CategoryService;
import es.uclm.esibuy.security.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void shouldListExistingCategoriesWithoutAuthentication() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of(new Category("category-id", "Electrónica")));

        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("category-id"))
                .andExpect(jsonPath("$[0].name").value("Electrónica"));
    }

    @Test
    void shouldKeepProductEndpointsProtected() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().is4xxClientError());
    }
}
