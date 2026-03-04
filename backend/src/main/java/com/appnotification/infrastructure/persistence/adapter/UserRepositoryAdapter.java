package com.appnotification.infrastructure.persistence.adapter;

import com.appnotification.application.port.output.UserRepositoryPort;
import com.appnotification.domain.entity.User;
import com.appnotification.infrastructure.persistence.entity.UserJpaEntity;
import com.appnotification.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpaRepository;

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public List<User> findAllExcept(UUID userId) {
        return jpaRepository.findAllByIdNot(userId).stream().map(this::toDomain).toList();
    }

    private User toDomain(UserJpaEntity e) {
        return new User(e.getId(), e.getUsername(), e.getEmail(), e.getPassword(), e.getCreatedAt());
    }

    private UserJpaEntity toEntity(User u) {
        UserJpaEntity e = new UserJpaEntity();
        e.setId(u.getId());
        e.setUsername(u.getUsername());
        e.setEmail(u.getEmail());
        e.setPassword(u.getPasswordHash());
        return e;
    }
}
