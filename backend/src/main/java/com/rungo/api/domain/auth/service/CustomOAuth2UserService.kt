package com.rungo.api.domain.auth.service

import com.rungo.api.domain.auth.entity.UserAuth
import com.rungo.api.domain.auth.repository.UserAuthRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Provider
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.repository.UserRepository
import jakarta.transaction.Transactional
import lombok.RequiredArgsConstructor
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import java.util.List
import java.util.function.Function
import java.util.function.Supplier

@Service
@RequiredArgsConstructor
@Transactional
class CustomOAuth2UserService : DefaultOAuth2UserService() {
    private val userRepository: UserRepository? = null
    private val userAuthRepository: UserAuthRepository? = null

    @Throws(OAuth2AuthenticationException::class)
    override fun loadUser(userRequest: OAuth2UserRequest?): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val attributes = oAuth2User.getAttributes()

        val email = attributes.get("email") as String
        var name = attributes.get("name") as String?
        val providerId = attributes.get("sub") as String?

        if (email == null) {
            throw RuntimeException("Email not found from OAuth2")
        }

        if (name == null || name.isBlank()) {
            name = email.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        }

        val finalName: String? = name

        val user = userAuthRepository!!.findByProviderAndProviderId(Provider.GOOGLE, providerId)
            .map<Users>(Function { obj: UserAuth? -> obj!!.getUser() })
            .orElseGet(Supplier {
                val existingUser = userRepository!!.findByEmail(email)
                    .orElseGet(Supplier {
                        userRepository.save<Users?>(
                            Users.builder()
                                .email(email)
                                .name(finalName)
                                .role(Role.PARTICIPANT)
                                .build()
                        )
                    })
                if (!userAuthRepository.existsByUserAndProvider(existingUser, Provider.GOOGLE)) {
                    userAuthRepository.save<UserAuth?>(
                        UserAuth.createSocialAuth(existingUser, Provider.GOOGLE, providerId)
                    )
                }
                existingUser
            })

        return DefaultOAuth2User(
            List.of<SimpleGrantedAuthority?>(SimpleGrantedAuthority("ROLE_" + user.getRole().name)),
            attributes,
            "email"
        )
    }
}