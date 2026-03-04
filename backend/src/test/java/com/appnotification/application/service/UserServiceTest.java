package com.appnotification.application.service;

import com.appnotification.application.port.input.user.ListUsersPort;
import com.appnotification.application.port.output.UserRepositoryPort;
import com.appnotification.application.usecase.user.ListUsersUseCase;
import com.appnotification.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private ListUsersUseCase listUsersUseCase;

    private User mockUserA;
    private User mockUserB;

    @BeforeEach
    void setUp() {
        mockUserA = new User(UUID.randomUUID(), "joao", "joao@email.com", "hashed", Instant.now());
        mockUserB = new User(UUID.randomUUID(), "maria", "maria@email.com", "hashed", Instant.now());
    }

    @Test
    void execute_shouldReturnAllUsersExceptCurrent() {
        when(userRepository.findAllExcept(mockUserA.getId())).thenReturn(List.of(mockUserB));

        List<ListUsersPort.Result> actualResult = listUsersUseCase.execute(mockUserA.getId());

        assertThat(actualResult).hasSize(1);
        assertThat(actualResult.get(0).username()).isEqualTo("maria");
        assertThat(actualResult.get(0).email()).isEqualTo("maria@email.com");
    }

    @Test
    void execute_shouldReturnEmptyList_whenNoOtherUsersExist() {
        when(userRepository.findAllExcept(mockUserA.getId())).thenReturn(List.of());

        List<ListUsersPort.Result> actualResult = listUsersUseCase.execute(mockUserA.getId());

        assertThat(actualResult).isEmpty();
    }

    @Test
    void findById_shouldReturnUser_whenExists() {
        when(userRepository.findById(mockUserA.getId())).thenReturn(Optional.of(mockUserA));

        User actualUser = userRepository.findById(mockUserA.getId()).orElseThrow();

        assertThat(actualUser.getId()).isEqualTo(mockUserA.getId());
        assertThat(actualUser.getUsername()).isEqualTo("joao");
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThat(userRepository.findById(unknownId)).isEmpty();
    }
}
