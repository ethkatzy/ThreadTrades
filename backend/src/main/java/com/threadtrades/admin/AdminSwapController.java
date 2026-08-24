package com.threadtrades.admin;

import com.threadtrades.common.PagedResponse;
import com.threadtrades.swap.SwapStatus;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/swaps")
@Validated
public class AdminSwapController {

    private final AdminSwapService adminSwapService;

    public AdminSwapController(AdminSwapService adminSwapService) {
        this.adminSwapService = adminSwapService;
    }

    @GetMapping
    public PagedResponse<AdminSwapResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) SwapStatus status) {
        return adminSwapService.list(page, size, status);
    }

    @GetMapping("/{id}")
    public AdminSwapResponse get(@PathVariable Long id) {
        return adminSwapService.get(id);
    }

    @PostMapping("/{id}/force-resolve")
    public AdminSwapResponse forceResolve(@PathVariable Long id, @Valid @RequestBody ForceResolveSwapRequest request) {
        return adminSwapService.forceResolve(id, request.status());
    }
}
