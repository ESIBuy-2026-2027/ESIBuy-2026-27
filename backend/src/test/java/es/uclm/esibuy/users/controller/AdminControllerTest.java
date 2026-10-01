package es.uclm.esibuy.users.controller;

import java.time.Instant;
import java.util.List;

import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldListUsersForAdminWithoutExposingPasswordHash() throws Exception {
        User user = mock(User.class);
        when(user.getId()).thenReturn("user-1");
        when(user.getEmail()).thenReturn("client@example.com");
        when(user.getFirstName()).thenReturn("Ana");
        when(user.getLastName()).thenReturn("López");
        when(user.getNationalId()).thenReturn("12345678Z");
        when(user.getPhone()).thenReturn("600123456");
        when(user.getProfileImage()).thenReturn("avatar-client-default");
        when(user.getRole()).thenReturn(UserRole.CLIENT);
        when(user.isActive()).thenReturn(true);
        when(user.getCreatedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(userService.findAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/admin/usuarios")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("user-1"))
                .andExpect(jsonPath("$[0].email").value("client@example.com"))
                .andExpect(jsonPath("$[0].firstName").value("Ana"))
                .andExpect(jsonPath("$[0].role").value("CLIENT"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAuthenticatedNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios")
                        .with(user("seller@example.com").roles("SELLER")))
                .andExpect(status().isForbidden());
    }
}
