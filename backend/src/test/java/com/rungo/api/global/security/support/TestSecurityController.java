package com.rungo.api.global.security.support;

import com.rungo.api.global.security.SecurityUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestSecurityController {

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal SecurityUser securityUser) {
        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of(
                "id", securityUser.getId(),
                "email", securityUser.getEmail(),
                "role", securityUser.getRole().name()
        ));
    }
}