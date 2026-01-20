package com.taskqueue.www.controller;

import com.taskqueue.www.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @PutMapping("/users/{id}/promote")
    public ResponseEntity<?> promote(@PathVariable Long id) {
        adminService.promoteToAdmin(id);
        return ResponseEntity.ok("User promoted to ADMIN");
    }

    @PutMapping("/users/{id}/demote")
    public ResponseEntity<?> demote(@PathVariable Long id) {
        adminService.demoteToUser(id);
        return ResponseEntity.ok("User demoted to USER");
    }
}
