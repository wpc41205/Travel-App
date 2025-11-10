package com.techup.travel_app.controller;

import com.techup.travel_app.dto.LoginRequest;
import com.techup.travel_app.dto.LoginResponse;
import com.techup.travel_app.dto.RegisterRequest;
import com.techup.travel_app.dto.RegisterResponse;
import com.techup.travel_app.dto.TokenRefreshRequest;
import com.techup.travel_app.dto.TokenRefreshResponse;
import com.techup.travel_app.dto.UserResponse;
import com.techup.travel_app.service.AuthenticationService;
import com.techup.travel_app.service.RefreshTokenService;
import com.techup.travel_app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse userResponse = userService.createUser(request);
        
        RegisterResponse response = RegisterResponse.builder()
                .message("Account created successfully. Please log in.")
                .user(userResponse)
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = refreshTokenService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}

