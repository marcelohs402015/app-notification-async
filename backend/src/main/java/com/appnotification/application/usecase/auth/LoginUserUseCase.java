package com.appnotification.application.usecase.auth;

import com.appnotification.application.port.input.auth.LoginUserPort;
import com.appnotification.application.port.output.PasswordEncoderPort;
import com.appnotification.application.port.output.TokenGeneratorPort;
import com.appnotification.application.port.output.UserRepositoryPort;
import com.appnotification.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginUserUseCase implements LoginUserPort {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenGeneratorPort tokenGenerator;

    @Transactional(readOnly = true)
    @Override
    public Result execute(Command command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = tokenGenerator.generate(user.getId(), user.getEmail());
        return new Result(token, user.getId().toString(), user.getUsername(),
                user.getEmail(), user.getCreatedAt());
    }
}
