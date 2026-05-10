package org.example.sapsanuserservice.repository.internal;

import org.example.sapsanuserservice.entity.internal.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByKeycloakId(UUID keycloakId);
    @Query(
            "SELECT u FROM User u JOIN StudentGroupHistory h ON h.user = u " +
                    "WHERE h.group.number = :groupNumber AND h.active = true"
    )
    List<User> findAllByGroupNumber(String groupNumber);
}
