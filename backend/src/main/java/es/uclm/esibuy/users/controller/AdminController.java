package es.uclm.esibuy.users.controller;

import java.util.List;

import es.uclm.esibuy.users.dto.AdminUserResponse;
import es.uclm.esibuy.users.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/usuarios")
    public List<AdminUserResponse> listUsers() {
        return userService.findAllUsers().stream()
                .map(AdminUserResponse::from)
                .toList();
    }
}
