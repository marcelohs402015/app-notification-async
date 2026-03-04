package com.appnotification.presentation.adapter.controller;

import com.appnotification.application.port.input.user.ListUsersPort;
import com.appnotification.domain.entity.User;
import com.appnotification.presentation.adapter.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final ListUsersPort listUsers;

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                listUsers.execute(currentUser.getId()).stream()
                        .map(r -> new UserResponse(r.id(), r.username(), r.email(), r.createdAt()))
                        .toList()
        );
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(new UserResponse(
                currentUser.getId(), currentUser.getUsername(),
                currentUser.getEmail(), currentUser.getCreatedAt()));
    }
}
