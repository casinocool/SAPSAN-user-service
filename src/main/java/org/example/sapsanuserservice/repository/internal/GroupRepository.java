package org.example.sapsanuserservice.repository.internal;

import org.example.sapsanuserservice.entity.internal.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group,Long> {
    Optional<Group> findByNumber(String number);
}
