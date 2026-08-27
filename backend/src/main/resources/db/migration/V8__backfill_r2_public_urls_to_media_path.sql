-- R2StorageService now hands out "/media/{key}" instead of R2's public
-- "pub-*.r2.dev" URL (see ROADMAP.md #3 -- that URL 404s some objects even
-- with public access enabled and correctly configured). Rewrite any rows
-- persisted before this change so already-uploaded images keep working.
UPDATE clothing_item
SET image_url = '/media/' || substring(image_url FROM '[^/]+$')
WHERE image_url LIKE 'https://pub-%.r2.dev/%';

UPDATE user_profile
SET profile_picture_url = '/media/' || substring(profile_picture_url FROM '[^/]+$')
WHERE profile_picture_url LIKE 'https://pub-%.r2.dev/%';

UPDATE message
SET image_url = '/media/' || substring(image_url FROM '[^/]+$')
WHERE image_url LIKE 'https://pub-%.r2.dev/%';
