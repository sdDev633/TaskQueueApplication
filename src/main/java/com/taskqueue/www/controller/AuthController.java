package com.taskqueue.www.controller;

import com.taskqueue.www.dto.LoginRequest;
import com.taskqueue.www.dto.RegisterRequest;
import com.taskqueue.www.enums.Role;
import com.taskqueue.www.jwt.JwtUtil;
import com.taskqueue.www.model.UserModel;
import com.taskqueue.www.repository.UserRepository;
import com.taskqueue.www.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    @PostMapping("/register")
    public void register(@RequestBody RegisterRequest req) {

        UserModel user = new UserModel();
        user.setUsername(req.getUsername());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRole(Role.ADMIN); // or USER

        userRepo.save(user);
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest req) {

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        req.getUsername(),
                        req.getPassword()
                )
        );

        String token = jwtUtil.generateToken(req.getUsername());
        return Map.of("token", token);
    }
}
