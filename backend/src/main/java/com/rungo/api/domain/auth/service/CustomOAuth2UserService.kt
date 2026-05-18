package com.rungo.api.domain.auth.service

import com.rungo.api.domain.auth.entity.UserAuth
import com.rungo.api.domain.auth.repository.UserAuthRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Provider
import com.rungo.api.domain.users.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
    private val userAuthRepository: UserAuthRepository
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val attributes = oAuth2User.attributes

        val email = attributes["email"] as? String
            ?: throw OAuth2AuthenticationException(OAuth2Error("email_not_found"), "Email not found from OAuth2")

        val providerId = attributes["sub"] as? String
            ?: throw OAuth2AuthenticationException(OAuth2Error("provider_id_not_found"), "Provider ID not found")

        val name = (attributes["name"] as? String)
            ?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")

        val userAuth = userAuthRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId)

        val user = if (userAuth != null) {
            userAuth.user
        } else {
            val existingUser = userRepository.findByEmail(email).orElse(null)
                ?: userRepository.save(
                    Users.createOAuth(
                        email = email,
                        name = name,
                    )
                )

            if (!userAuthRepository.existsByUserAndProvider(existingUser, Provider.GOOGLE)) {
                userAuthRepository.save(
                    UserAuth.createSocialAuth(existingUser, Provider.GOOGLE, providerId)
                )
            }
            existingUser
        }

        return DefaultOAuth2User(
            listOf(SimpleGrantedAuthority("ROLE_${user.role.name}")),
            attributes,
            "email"
        )
    }
}