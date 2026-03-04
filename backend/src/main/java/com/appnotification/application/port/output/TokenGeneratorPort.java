package com.appnotification.application.port.output;

import java.util.UUID;

public interface TokenGeneratorPort {
    String generate(UUID userId, String email);
}
