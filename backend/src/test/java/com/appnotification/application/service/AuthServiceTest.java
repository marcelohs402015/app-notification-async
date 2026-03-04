package com.appnotification.application.service;

import com.appnotification.application.port.input.auth.LoginUserPort;
import com.appnotification.application.port.input.auth.RegisterUserPort;
import com.appnotification.application.port.output.PasswordEncoderPort;
import com.appnotification.application.port.output.TokenGeneratorPort;
import com.appnotification.application.port.output.UserRepositoryPort;
import com.appnotification.application.usecase.auth.LoginUserUseCase;
import com.appnotification.application.usecase.auth.RegisterUserUseCase;
import com.appnotification.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private PasswordEncoderPort passwordEncoder;
    @Mock
    private TokenGeneratorPort tokenGenerator;

    @InjectMocks
    private RegisterUserUseCase registerUserUseCase;

    private LoginUserUseCase loginUserUseCase;

    private User mockUser;

    @BeforeEach
    void setUp() {
        loginUserUseCase = new LoginUserUseCase(userRepository, passwordEncoder, tokenGenerator);
        mockUser = new User(UUID.randomUUID(), "joao", "joao@email.com", "hashed_password", Instant.now());
    }

    @Test
    void register_shouldReturnResult_whenCredentialsAreUnique() {
        RegisterUserPort.Command inputCommand = new RegisterUserPort.Command("joao", "joao@email.com", "senha123");
        when(userRepository.existsByEmail(inputCommand.email())).thenReturn(false);
        when(userRepository.existsByUsername(inputCommand.username())).thenReturn(false);
        when(passwordEncoder.encode(inputCommand.password())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(tokenGenerator.generate(mockUser.getId(), mockUser.getEmail())).thenReturn("mock_token");

        RegisterUserPort.Result actualResult = registerUserUseCase.execute(inputCommand);

        assertThat(actualResult.token()).isEqualTo("mock_token");
        assertThat(actualResult.email()).isEqualTo("joao@email.com");
        assertThat(actualResult.username()).isEqualTo("joao");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed_password");
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyInUse() {
        RegisterUserPort.Command inputCommand = new RegisterUserPort.Command("joao", "joao@email.com", "senha123");
        when(userRepository.existsByEmail(inputCommand.email())).thenReturn(true);

        assertThatThrownBy(() -> registerUserUseCase.execute(inputCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldThrow_whenUsernameAlreadyInUse() {
        RegisterUserPort.Command inputCommand = new RegisterUserPort.Command("joao", "joao@email.com", "senha123");
        when(userRepository.existsByEmail(inputCommand.email())).thenReturn(false);
        when(userRepository.existsByUsername(inputCommand.username())).thenReturn(true);

        assertThatThrownBy(() -> registerUserUseCase.execute(inputCommand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldReturnResult_whenCredentialsAreValid() {
        LoginUserPort.Command inputCommand = new LoginUserPort.Command("joao@email.com", "senha123");
        when(userRepository.findByEmail(inputCommand.email())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(inputCommand.password(), mockUser.getPasswordHash())).thenReturn(true);
        when(tokenGenerator.generate(mockUser.getId(), mockUser.getEmail())).thenReturn("mock_token");

        LoginUserPort.Result actualResult = loginUserUseCase.execute(inputCommand);

        assertThat(actualResult.token()).isEqualTo("mock_token");
        assertThat(actualResult.email()).isEqualTo("joao@email.com");
    }

    @Test
    void login_shouldThrowBadCredentials_whenUserNotFound() {
        LoginUserPort.Command inputCommand = new LoginUserPort.Command("naoexiste@email.com", "senha123");
        when(userRepository.findByEmail(inputCommand.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginUserUseCase.execute(inputCommand))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_shouldThrowBadCredentials_whenPasswordDoesNotMatch() {
        LoginUserPort.Command inputCommand = new LoginUserPort.Command("joao@email.com", "senha_errada");
        when(userRepository.findByEmail(inputCommand.email())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(inputCommand.password(), mockUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> loginUserUseCase.execute(inputCommand))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(tokenGenerator, never()).generate(any(), anyString());
    }
}
