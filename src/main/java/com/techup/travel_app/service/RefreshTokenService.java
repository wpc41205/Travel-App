package com.techup.travel_app.service;

import com.techup.travel_app.dto.TokenRefreshResponse;
import com.techup.travel_app.entity.RefreshToken;
import com.techup.travel_app.entity.User;
import com.techup.travel_app.exception.TokenRefreshException;
import com.techup.travel_app.repository.RefreshTokenRepository;
import com.techup.travel_app.repository.UserRepository;
import com.techup.travel_app.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-token-expiration:604800000}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Delete existing refresh token for this user if exists
        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);

        // Generate refresh token
        String tokenValue = jwtUtil.generateRefreshToken(user.getEmail());
        
        refreshToken.setUser(user);
        refreshToken.setToken(tokenValue);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    public TokenRefreshResponse refreshToken(String requestRefreshToken) {
        return findByToken(requestRefreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    // Generate new access token
                    String newAccessToken = jwtUtil.generateAccessToken(user.getEmail());
                    
                    // Generate new refresh token and update database
                    RefreshToken newRefreshToken = createRefreshToken(user.getId());
                    
                    return TokenRefreshResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .type("Bearer")
                            .build();
                })
                .orElseThrow(() -> new TokenRefreshException("Refresh token is not in database!"));
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return refreshTokenRepository.deleteByUser(user);
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
}

