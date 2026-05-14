package com.rungo.api.domain.auth.service;

import com.rungo.api.domain.auth.entity.UserAuth;
import com.rungo.api.domain.auth.repository.UserAuthRepository;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Provider;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.domain.users.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String providerId = (String) attributes.get("sub");

        if (email == null) {
            throw new RuntimeException("Email not found from OAuth2");
        }

        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }

        final String finalName = name;

        Users user = userAuthRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId)
                                       .map(UserAuth::getUser)
                                       .orElseGet(() -> {
                                           Users existingUser = userRepository.findByEmail(email)
                                                                              .orElseGet(() -> userRepository.save(
                                                                                      Users.builder()
                                                                                           .email(email)
                                                                                           .name(finalName)
                                                                                           .role(Role.PARTICIPANT)
                                                                                           .build()
                                                                              ));

                                           if (!userAuthRepository.existsByUserAndProvider(existingUser, Provider.GOOGLE)) {
                                               userAuthRepository.save(
                                                       UserAuth.createSocialAuth(existingUser, Provider.GOOGLE, providerId)
                                               );
                                           }

                                           return existingUser;
                                       });

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes,
                "email"
        );
    }
}