package com.threadtrades.admin;

import com.threadtrades.common.PagedResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/matches")
public class AdminMatchController {

    private final AdminMatchService adminMatchService;

    public AdminMatchController(AdminMatchService adminMatchService) {
        this.adminMatchService = adminMatchService;
    }

    @GetMapping
    public PagedResponse<AdminMatchResponse> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return adminMatchService.list(page, size);
    }

    @GetMapping("/{id}")
    public AdminMatchResponse get(@PathVariable Long id) {
        return adminMatchService.get(id);
    }
}
