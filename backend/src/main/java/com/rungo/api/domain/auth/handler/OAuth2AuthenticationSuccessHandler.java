package com.rungo.api.domain.auth.handler;

import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.repository.UserRepository;
import com.rungo.api.global.util.CookieUtil;
import com.rungo.api.global.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = (String) oAuth2User.getAttributes().get("email");

        Users user = userRepository.findByEmail(email)
                                   .orElseThrow();

        String accessToken = JwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                secret
        );

        String refreshToken = JwtUtil.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                secret
        );

        CookieUtil.addCookie(response, "accessToken", accessToken, 60 * 60);
        CookieUtil.addCookie(response, "refreshToken", refreshToken, 60 * 60 * 24 * 7);

        response.sendRedirect(frontendUrl);
    }
}