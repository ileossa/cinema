package com.cinema.cinema.domain.port.in;

public interface AuthUseCase {

    AuthResult login(String email, String password);

    AuthResult register(String email, String fullName, String password);

    record AuthResult(
            Long userId,
            String email,
            String fullName,
            String token
    ) {
    }
}
