package com.appnotification.application.port.input.auth;

public interface RegisterUserPort {
    record Command(String username, String email, String password) {}
    record Result(String token, String userId, String username, String email, java.time.Instant createdAt) {}

    Result execute(Command command);
}
