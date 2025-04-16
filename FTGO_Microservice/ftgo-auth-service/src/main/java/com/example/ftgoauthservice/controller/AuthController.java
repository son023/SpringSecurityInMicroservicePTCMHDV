package com.example.ftgoauthservice.controller;

import com.example.ftgoauthservice.config.JwtUtil;
import com.example.ftgoauthservice.model.AuthResponse;
import com.example.ftgoauthservice.model.Permission;
import com.example.ftgoauthservice.model.Role;
import com.example.ftgoauthservice.model.User;
import com.example.ftgoauthservice.repository.RoleRepository;
import com.example.ftgoauthservice.repository.UserRepository;
import com.example.ftgoauthservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;


    private  final RoleRepository roleRepository;

    private final  PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @GetMapping("role")
    public ResponseEntity<?> getRole(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("token");
        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userService.loadUserByUsername(username);
        String roles = "";
        User user = userService.findByUsername(userDetails.getUsername());
        if (user != null) {
            for (Role role : user.getRoles()) {
                roles += role.getName() + ",";
                for (Permission x : role.getPermissions()) {
                    roles += x.getName() + ",";
                }


            }
            return ResponseEntity.ok(roles);

        }
        return null;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> signupRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(signupRequest.get("username"), signupRequest.get("password"))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String jwt = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());

            String roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));

            User member = userService.findByUsername(userDetails.getUsername());

            AuthResponse response = new AuthResponse(
                    jwt,
                    refreshToken,
                    member.getUsername(),
                    "full name",
                    roles
            );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> signupRequest) {
        if (userService.existsByUsername(signupRequest.get("username"))) {
            return ResponseEntity.badRequest().body("Tên đăng nhập đã được sử dụng!");
        }


        User user = new User();
        user.setUsername(signupRequest.get("username"));
        user.setPassword(passwordEncoder.encode(signupRequest.get("password")));
        log.error("ok");

        Optional<Role> userRole = roleRepository.findByName("ROLE_USER");
        if (userRole.isEmpty()) {
            Role role = new Role();
            role.setName("ROLE_USER");
            role = roleRepository.save(role);
            user.setRoles(Collections.singleton(role));
        } else {
            user.setRoles(Collections.singleton(userRole.get()));
        }

        userService.save(user);

        return ResponseEntity.ok("Đăng ký người dùng thành công!");
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (jwtUtil.validateRefreshToken(refreshToken)) {
            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userService.loadUserByUsername(username);
            String accessToken = jwtUtil.generateToken(userDetails);
            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken, userDetails.getUsername(), "full name", getRoles(userDetails)));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }

    private String getRoles(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }
}