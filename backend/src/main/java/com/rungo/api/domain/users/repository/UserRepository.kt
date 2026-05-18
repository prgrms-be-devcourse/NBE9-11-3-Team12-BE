package com.rungo.api.domain.users.repository

import com.rungo.api.domain.users.entity.Users
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<Users, Long> {

    fun findByEmail(email: String): Users?

    fun findAllByEmailStartingWith(prefix: String): List<Users>
}