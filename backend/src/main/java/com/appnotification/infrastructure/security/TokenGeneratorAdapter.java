package com.appnotification.infrastructure.security;

import com.appnotification.application.port.output.TokenGeneratorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TokenGeneratorAdapter implements TokenGeneratorPort {

    private final JwtService jwtService;

    @Override
    public String generate(UUID userId, String email) {
        return jwtService.generateToken(userId, email);
    }
}
