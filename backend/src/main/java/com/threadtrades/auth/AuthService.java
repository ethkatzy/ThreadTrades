package com.threadtrades.auth;

import com.threadtrades.security.JwtService;
import com.threadtrades.user.AppUser;
import com.threadtrades.user.AppUserRepository;
import com.threadtrades.user.UserProfile;
import com.threadtrades.user.UserProfileRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository appUserRepository,
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException(request.email());
        }
        if (userProfileRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyInUseException(request.username());
        }

        AppUser appUser = appUserRepository.save(
                new AppUser(request.email(), passwordEncoder.encode(request.password())));
        UserProfile profile = userProfileRepository.save(
                new UserProfile(appUser, request.username(), request.name()));

        String token = jwtService.generateToken(appUser);
        return new AuthResponse(token, appUser.getId(), profile.getUsername(), profile.getName());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser appUser = appUserRepository
                .findByEmail(request.email())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        UserProfile profile = userProfileRepository.findByAppUserId(appUser.getId()).orElseThrow();

        String token = jwtService.generateToken(appUser);
        return new AuthResponse(token, appUser.getId(), profile.getUsername(), profile.getName());
    }
}
