package es.uclm.esibuy.categories.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.uclm.esibuy.categories.dto.CategoryRequest;
import es.uclm.esibuy.categories.model.Category;
import es.uclm.esibuy.categories.service.CategoryService;
import es.uclm.esibuy.common.exception.CategoryIdNotFoundException;
import es.uclm.esibuy.common.exception.CategoryInUseException;
import es.uclm.esibuy.common.exception.DuplicateCategoryException;
import es.uclm.esibuy.security.config.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminCategoryController.class)
@Import(SecurityConfig.class)
class AdminCategoryControllerTest {

        private static final String API = "/api/admin/categorias";
        private static final String CATEGORY_ID = "category-1";

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CategoryService categoryService;

        @Test
        void adminCanListAndCreateCategories() throws Exception {
                when(categoryService.findAll()).thenReturn(List.of(new Category(CATEGORY_ID, "Electrónica")));
                when(categoryService.create(any(CategoryRequest.class)))
                                .thenReturn(new Category(CATEGORY_ID, "Papelería"));

                mockMvc.perform(get(API).with(user("admin").roles("ADMIN")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("Electrónica"));

                mockMvc.perform(post(API)
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Papelería\"}"))
                                .andExpect(status().isCreated())
                                .andExpect(header().exists("X-Correlation-ID"))
                                .andExpect(jsonPath("$.id").value(CATEGORY_ID))
                                .andExpect(jsonPath("$.name").value("Papelería"));
        }

        @Test
        void adminCanUpdateAndDeleteCategories() throws Exception {
                when(categoryService.update(eq(CATEGORY_ID), any(CategoryRequest.class)))
                                .thenReturn(new Category(CATEGORY_ID, "Hogar"));

                mockMvc.perform(put(API + "/" + CATEGORY_ID)
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Hogar\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Hogar"));

                mockMvc.perform(delete(API + "/" + CATEGORY_ID)
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                verify(categoryService).delete(CATEGORY_ID);
        }

        @Test
        void rejectsClientsAndSellers() throws Exception {
                mockMvc.perform(get(API).with(user("client").roles("CLIENT")))
                                .andExpect(status().isForbidden());
                mockMvc.perform(post(API)
                                .with(user("seller").roles("SELLER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Hogar\"}"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void rejectsAnonymousRequests() throws Exception {
                mockMvc.perform(get(API))
                                .andExpect(status().isUnauthorized())
                                .andExpect(header().exists("X-Correlation-ID"));
                mockMvc.perform(post(API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Hogar\"}"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void validatesCategoryNameAndReturnsFieldErrors() throws Exception {
                mockMvc.perform(post(API)
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\" \"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.errors.name").exists())
                                .andExpect(jsonPath("$.correlationId").isNotEmpty());
        }

        @Test
        void returnsConflictWhenCategoryNameAlreadyExists() throws Exception {
                when(categoryService.create(any(CategoryRequest.class))).thenThrow(new DuplicateCategoryException());

                mockMvc.perform(post(API)
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Hogar\"}"))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Ya existe una categoría con ese nombre"))
                                .andExpect(jsonPath("$.correlationId").isNotEmpty());
        }

        @Test
        void mapsMissingAndInUseCategoriesToExpectedStatuses() throws Exception {
                when(categoryService.update(eq(CATEGORY_ID), any(CategoryRequest.class)))
                                .thenThrow(new CategoryIdNotFoundException());
                doThrow(new CategoryInUseException()).when(categoryService).delete(CATEGORY_ID);

                mockMvc.perform(put(API + "/" + CATEGORY_ID)
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Hogar\"}"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.correlationId").isNotEmpty());

                mockMvc.perform(delete(API + "/" + CATEGORY_ID)
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value(
                                                "La categoría está en uso por productos y no se puede eliminar"));
        }
}
