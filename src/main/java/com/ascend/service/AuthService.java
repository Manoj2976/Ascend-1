package com.ascend.service;

import com.ascend.config.AscendProperties;
import com.ascend.domain.User;
import com.ascend.domain.UserSettings;
import com.ascend.dto.AuthDtos.AuthResponse;
import com.ascend.dto.AuthDtos.LoginRequest;
import com.ascend.dto.AuthDtos.RegisterRequest;
import com.ascend.repo.UserRepository;
import com.ascend.repo.UserSettingsRepository;
import com.ascend.security.JwtService;
import com.ascend.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final UserSettingsRepository settingsRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AscendProperties props;

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        if (!props.allowRegistration()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Registration is closed");
        }
        String email = normalize(r.email());
        if (users.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT, "That email is already registered");

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        users.save(u);

        UserSettings s = new UserSettings(u.getId());
        s.setName(r.name() == null ? "" : r.name().trim());
        settingsRepo.save(s);
        return new AuthResponse(jwt.issue(u.getId()), u.getEmail(), s.getName());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail(normalize(r.email()))
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));
        String name = settingsRepo.findById(u.getId()).map(UserSettings::getName).orElse("");
        return new AuthResponse(jwt.issue(u.getId()), u.getEmail(), name);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
