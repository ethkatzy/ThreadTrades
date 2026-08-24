package com.threadtrades.admin;

import com.threadtrades.clothing.ClothingItemResponse;
import com.threadtrades.clothing.ItemStatus;
import com.threadtrades.common.PagedResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/clothing-items")
public class AdminClothingItemController {

    private final AdminClothingItemService adminClothingItemService;

    public AdminClothingItemController(AdminClothingItemService adminClothingItemService) {
        this.adminClothingItemService = adminClothingItemService;
    }

    @GetMapping
    public PagedResponse<ClothingItemResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ItemStatus status) {
        return adminClothingItemService.list(page, size, status);
    }

    @GetMapping("/{id}")
    public ClothingItemResponse get(@PathVariable Long id) {
        return adminClothingItemService.get(id);
    }

    @PostMapping("/{id}/remove")
    public ClothingItemResponse remove(@PathVariable Long id) {
        return adminClothingItemService.remove(id);
    }
}
