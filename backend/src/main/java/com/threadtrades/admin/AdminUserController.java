package com.threadtrades.admin;

import com.threadtrades.common.PagedResponse;
import com.threadtrades.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public PagedResponse<AdminUserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String query) {
        return adminUserService.list(page, size, query);
    }

    @GetMapping("/{id}")
    public AdminUserResponse get(@PathVariable Long id) {
        return adminUserService.get(id);
    }

    @PostMapping("/{id}/ban")
    public AdminUserResponse ban(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return adminUserService.ban(id, currentUser.appUserId());
    }

    @PostMapping("/{id}/unban")
    public AdminUserResponse unban(@PathVariable Long id) {
        return adminUserService.unban(id);
    }
}
