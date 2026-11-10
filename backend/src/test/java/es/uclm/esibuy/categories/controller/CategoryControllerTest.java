package es.uclm.esibuy.categories.controller;

import java.util.List;

import es.uclm.esibuy.categories.model.Category;
import es.uclm.esibuy.categories.service.CategoryService;
import es.uclm.esibuy.security.config.SecurityConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @MockBean
    private CategoryService categoryService;

    @Test
    void shouldListExistingCategoriesWithoutAuthentication() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of(new Category("category-id", "Electrónica")));

        var result = mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("category-id"))
                .andExpect(jsonPath("$[0].name").value("Electrónica"))
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);
        assertFalse(csrfCookie.isHttpOnly());
        assertTrue(result.getResponse().getHeader("Set-Cookie").contains("Secure"));
        assertEquals("true", environment.getProperty("server.servlet.session.cookie.http-only"));
        assertEquals("Lax", environment.getProperty("server.servlet.session.cookie.same-site"));
        assertEquals("true", environment.getProperty("server.servlet.session.cookie.secure"));
    }

    @Test
    void shouldKeepProductEndpointsProtected() throws Exception {
        mockMvc.perform(get("/api/products/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void shouldRejectNonSellerFromProductEndpoints() throws Exception {
        mockMvc.perform(get("/api/products/mine"))
                .andExpect(status().isForbidden());
    }
}
