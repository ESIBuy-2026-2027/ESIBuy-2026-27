package es.uclm.esibuy.users.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.security.service.LoginAttemptService;
import es.uclm.esibuy.security.service.SessionAuthenticationService;
import es.uclm.esibuy.users.dto.LoginRequest;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.service.AdminUserService;
import es.uclm.esibuy.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = { UserController.class, AdminUserController.class })
@Import({ SecurityConfig.class, SessionAuthenticationService.class })
class UserSessionAuthenticationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private UserService userService;

        @MockitoBean
        private AdminUserService adminUserService;

        @MockitoBean
        private LoginAttemptService loginAttemptService;

        @Test
        void adminLoginCreatesSessionAcceptedByAdminEndpoint() throws Exception {
                User admin = user("admin-1", UserRole.ADMIN);
                when(userService.login(any(LoginRequest.class))).thenReturn(admin);
                when(adminUserService.obtenerPorId("admin-1")).thenReturn(admin);

                MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email":"admin@example.com","password":"AdminPassword123!"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.role").value("ADMIN"))
                                .andReturn();

                MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
                mockMvc.perform(get("/api/v1/admin/users/admin-1").session(session))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.email").value("admin@example.com"));
        }

        @Test
        void clientLoginCannotAccessAdminEndpoint() throws Exception {
                User client = user("client-1", UserRole.CLIENT);
                when(userService.login(any(LoginRequest.class))).thenReturn(client);

                MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"email":"client@example.com","password":"ClientPassword123!"}
                                                """))
                                .andExpect(status().isOk())
                                .andReturn();

                MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
                mockMvc.perform(get("/api/v1/admin/users/client-1").session(session))
                                .andExpect(status().isForbidden());
        }

        private User user(String id, UserRole role) {
                User user = new User(role == UserRole.ADMIN ? "admin@example.com" : "client@example.com",
                                "Test", "User", role);
                ReflectionTestUtils.setField(user, "id", id);
                return user;
        }
}