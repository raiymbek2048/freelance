package kg.freelance.service;

import kg.freelance.dto.request.LoginRequest;
import kg.freelance.dto.request.RefreshTokenRequest;
import kg.freelance.dto.request.RegisterRequest;
import kg.freelance.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);
}
