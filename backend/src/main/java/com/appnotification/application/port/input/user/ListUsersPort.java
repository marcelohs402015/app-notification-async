package com.appnotification.application.port.input.user;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ListUsersPort {
    record Result(UUID id, String username, String email, Instant createdAt) {}

    List<Result> execute(UUID currentUserId);
}
