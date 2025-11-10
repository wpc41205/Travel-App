package com.techup.travel_app.service;

import com.techup.travel_app.dto.LoginRequest;
import com.techup.travel_app.dto.LoginResponse;
import com.techup.travel_app.entity.RefreshToken;
import com.techup.travel_app.entity.User;
import com.techup.travel_app.repository.UserRepository;
import com.techup.travel_app.security.CustomUserDetails;
import com.techup.travel_app.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            
            // Generate access token (short-lived, 15-30 min)
            String accessToken = jwtUtil.generateAccessToken(userDetails);

            // Get user and create refresh token (long-lived, 7 days)
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

            // Build user info
            LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .build();

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .type("Bearer")
                    .user(userInfo)
                    .build();
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }
}

