package com.taskqueue.www.service;

import com.taskqueue.www.enums.Role;
import com.taskqueue.www.model.UserModel;
import com.taskqueue.www.repository.UserRepository;
import com.taskqueue.www.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    @Transactional
    public void promoteToAdmin(Long userId) {

        Long currentAdminId = SecurityUtils.currentUserId();

        if (userId.equals(currentAdminId)) {
            throw new RuntimeException("Admin cannot change own role");
        }

        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("User is already ADMIN");
        }

        user.setRole(Role.ADMIN);
    }

    @Transactional
    public void demoteToUser(Long userId) {

        Long currentAdminId = SecurityUtils.currentUserId();

        if (userId.equals(currentAdminId)) {
            throw new RuntimeException("Admin cannot demote self");
        }

        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.USER) {
            throw new RuntimeException("User is already USER");
        }

        user.setRole(Role.USER);
    }
}
