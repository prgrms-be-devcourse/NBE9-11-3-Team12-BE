package com.rungo.api.domain.auth.entity;

import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Provider;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "user_auth",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_provider_provider_id",
                        columnNames = {"provider", "provider_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(length = 255)
    private String password;

    public static UserAuth createLocalAuth(Users user, String encodedPassword) {
        return UserAuth.builder()
                       .user(user)
                       .provider(Provider.LOCAL)
                       .providerId(user.getEmail())
                       .password(encodedPassword)
                       .build();
    }

    public static UserAuth createSocialAuth(Users user, Provider provider, String providerId) {
        return UserAuth.builder()
                       .user(user)
                       .provider(provider)
                       .providerId(providerId)
                       .password(null)
                       .build();
    }
}