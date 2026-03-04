package com.appnotification.application.usecase.auth;

import com.appnotification.application.port.input.auth.RegisterUserPort;
import com.appnotification.application.port.output.PasswordEncoderPort;
import com.appnotification.application.port.output.TokenGeneratorPort;
import com.appnotification.application.port.output.UserRepositoryPort;
import com.appnotification.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase implements RegisterUserPort {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenGeneratorPort tokenGenerator;

    @Transactional
    @Override
    public Result execute(Command command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (userRepository.existsByUsername(command.username())) {
            throw new IllegalArgumentException("Username already in use");
        }

        User user = new User(null, command.username(), command.email(),
                passwordEncoder.encode(command.password()), Instant.now());

        User saved = userRepository.save(user);
        String token = tokenGenerator.generate(saved.getId(), saved.getEmail());

        return new Result(token, saved.getId().toString(), saved.getUsername(),
                saved.getEmail(), saved.getCreatedAt());
    }
}
