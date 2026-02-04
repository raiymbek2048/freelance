package kg.freelance.service;

import kg.freelance.dto.request.UserUpdateRequest;
import kg.freelance.dto.response.UserResponse;
import kg.freelance.entity.User;
import kg.freelance.security.UserPrincipal;

public interface UserService {

    UserPrincipal loadUserById(Long id);

    UserPrincipal loadUserByEmail(String email);

    User findById(Long id);

    User findByEmail(String email);

    UserResponse getCurrentUser(Long userId);

    UserResponse updateUser(Long userId, UserUpdateRequest request);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
