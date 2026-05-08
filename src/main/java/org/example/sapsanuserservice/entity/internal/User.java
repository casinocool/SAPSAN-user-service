package org.example.sapsanuserservice.entity.internal;

import jakarta.persistence.*;
import lombok.*;
import org.example.sapsanuserservice.entity.enums.UserStatus;
import org.example.sapsanuserservice.entity.enums.UserType;

import java.util.UUID;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "keycloak_id",unique = true,nullable = false)
    private UUID keycloakId;

    @Column(unique = true,nullable = false)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type",nullable = false)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
}
