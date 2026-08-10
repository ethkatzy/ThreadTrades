package com.threadtrades.clothing;

import com.threadtrades.security.AuthenticatedUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/clothing-items")
@Validated
public class ClothingItemController {

    private final ClothingItemService clothingItemService;

    public ClothingItemController(ClothingItemService clothingItemService) {
        this.clothingItemService = clothingItemService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClothingItemResponse> upload(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestPart("image") MultipartFile image,
            @RequestParam @NotBlank @Size(max = 255) String name,
            @RequestParam(required = false) @Size(max = 255) String brand,
            @RequestParam @NotBlank @Size(max = 50) String itemType,
            @RequestParam(required = false) String description,
            @RequestParam @NotNull ClothingSize clothingSize,
            @RequestParam(required = false) @Size(max = 50) String colour,
            @RequestParam @NotNull Condition condition,
            @RequestParam @NotNull Gender gender) {
        ClothingItem item = clothingItemService.upload(
                currentUser.appUserId(),
                image,
                new UploadClothingItemRequest(
                        name, brand, itemType, description, clothingSize, colour, condition, gender));
        return ResponseEntity.status(HttpStatus.CREATED).body(ClothingItemResponse.from(item));
    }

    @GetMapping("/mine")
    public List<ClothingItemResponse> mine(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return clothingItemService.listOwnedBy(currentUser.appUserId()).stream()
                .map(ClothingItemResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ClothingItemResponse getById(@PathVariable Long id) {
        return ClothingItemResponse.from(clothingItemService.getById(id));
    }
}
