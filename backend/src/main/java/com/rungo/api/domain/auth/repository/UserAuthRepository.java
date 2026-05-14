package com.rungo.api.domain.auth.repository;

import com.rungo.api.domain.auth.entity.UserAuth;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Provider;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<UserAuth> findByProviderAndProviderId(Provider provider, String providerId);

    Optional<UserAuth> findByUser_EmailAndProvider(String email, Provider provider);

    boolean existsByUserAndProvider(Users user, Provider provider);
}