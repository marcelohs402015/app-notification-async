package com.appnotification.application.usecase.user;

import com.appnotification.application.port.input.user.ListUsersPort;
import com.appnotification.application.port.output.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListUsersUseCase implements ListUsersPort {

    private final UserRepositoryPort userRepository;

    @Transactional(readOnly = true)
    @Override
    public List<Result> execute(UUID currentUserId) {
        return userRepository.findAllExcept(currentUserId).stream()
                .map(u -> new Result(u.getId(), u.getUsername(), u.getEmail(), u.getCreatedAt()))
                .toList();
    }
}
