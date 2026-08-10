package com.threadtrades.clothing;

import com.threadtrades.storage.StorageService;
import com.threadtrades.user.UserProfile;
import com.threadtrades.user.UserProfileRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ClothingItemService {

    private final ClothingItemRepository clothingItemRepository;
    private final UserProfileRepository userProfileRepository;
    private final StorageService storageService;

    public ClothingItemService(
            ClothingItemRepository clothingItemRepository,
            UserProfileRepository userProfileRepository,
            StorageService storageService) {
        this.clothingItemRepository = clothingItemRepository;
        this.userProfileRepository = userProfileRepository;
        this.storageService = storageService;
    }

    @Transactional
    public ClothingItem upload(Long appUserId, MultipartFile image, UploadClothingItemRequest request) {
        UserProfile owner = ownerProfile(appUserId);
        String imageUrl = storageService.store(image);
        ClothingItem item = new ClothingItem(
                owner,
                request.name(),
                imageUrl,
                request.brand(),
                request.itemType(),
                request.description(),
                request.clothingSize(),
                request.colour(),
                request.condition(),
                request.gender());
        return clothingItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public ClothingItem getById(Long id) {
        return clothingItemRepository.findById(id).orElseThrow(() -> new ClothingItemNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<ClothingItem> listOwnedBy(Long appUserId) {
        UserProfile owner = ownerProfile(appUserId);
        return clothingItemRepository.findAllByOwnerId(owner.getId());
    }

    private UserProfile ownerProfile(Long appUserId) {
        return userProfileRepository
                .findByAppUserId(appUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated app user has no profile: " + appUserId));
    }
}
