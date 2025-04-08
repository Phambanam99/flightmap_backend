package com.phamnam.tracking_vessel_flight.service;

import com.phamnam.tracking_vessel_flight.models.BlacklistedToken;
import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.models.auth.AuthenticationRequest;
import com.phamnam.tracking_vessel_flight.models.auth.AuthenticationResponse;
import com.phamnam.tracking_vessel_flight.models.auth.LogoutRequest;
import com.phamnam.tracking_vessel_flight.models.auth.RefreshTokenRequest;
import com.phamnam.tracking_vessel_flight.models.auth.RegisterRequest;
import com.phamnam.tracking_vessel_flight.repository.BlacklistedTokenRepository;
import com.phamnam.tracking_vessel_flight.repository.UserRepository;
import com.phamnam.tracking_vessel_flight.exception.BadRequestException;
import com.phamnam.tracking_vessel_flight.service.interfaces.IAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthenticationService implements IAuthenticationService {

    private final UserRepository userRepository;
    private final BlacklistedTokenRepository tokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String username = jwtService.extractUsername(refreshToken);

        if (username != null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(refreshToken, userDetails) &&
                    !tokenRepository.existsByToken(refreshToken)) {
                String accessToken = jwtService.generateToken(userDetails);

                return AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
            }
        }

        throw new BadRequestException("Invalid refresh token");
    }

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String token = request.getAccessToken();

        // Skip if token is already blacklisted
        if (tokenRepository.existsByToken(token)) {
            return;
        }

        try {
            Date expiration = jwtService.extractExpiration(token);
            LocalDateTime expiryDate = expiration.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                    .token(token)
                    .expiryDate(expiryDate)
                    .build();

            tokenRepository.save(blacklistedToken);

            // Clean up expired tokens
            tokenRepository.deleteAllExpiredTokens(LocalDateTime.now());

        } catch (Exception e) {
            throw new BadRequestException("Invalid token");
        }
    }
}
