package com.appnotification.presentation.adapter.controller;

import com.appnotification.application.port.input.auth.LoginUserPort;
import com.appnotification.application.port.input.auth.RegisterUserPort;
import com.appnotification.presentation.adapter.dto.AuthResponse;
import com.appnotification.presentation.adapter.dto.LoginRequest;
import com.appnotification.presentation.adapter.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserPort registerUser;
    private final LoginUserPort loginUser;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserPort.Result result = registerUser.execute(
                new RegisterUserPort.Command(request.username(), request.email(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toAuthResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginUserPort.Result result = loginUser.execute(
                new LoginUserPort.Command(request.email(), request.password()));
        return ResponseEntity.ok(toAuthResponse(result));
    }

    private AuthResponse toAuthResponse(RegisterUserPort.Result result) {
        return new AuthResponse(result.token(),
                new AuthResponse.UserResponse(result.userId(), result.username(),
                        result.email(), result.createdAt()));
    }

    private AuthResponse toAuthResponse(LoginUserPort.Result result) {
        return new AuthResponse(result.token(),
                new AuthResponse.UserResponse(result.userId(), result.username(),
                        result.email(), result.createdAt()));
    }
}
