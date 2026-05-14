package com.rungo.api.domain.users.entity;

import com.rungo.api.domain.users.enumtype.Gender;
import com.rungo.api.domain.users.enumtype.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column
    private Gender gender;

    @Column
    private LocalDate birth;

    // 내 프로필 수정 (name, phoneNumber)
    public void updateProfile(String name, String phoneNumber, Gender gender, LocalDate birth) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.birth = birth;
    }

    //주최자로 승급
    public void promoteToOrganizer() {
        this.role = Role.ORGANIZER;
    }

    public boolean isProfileCompleted() {
        return phoneNumber != null && gender != null && birth != null;
    }
}