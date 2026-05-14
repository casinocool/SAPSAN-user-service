package org.example.sapsanuserservice.repository.internal;

import org.example.sapsanuserservice.entity.internal.Group;
import org.example.sapsanuserservice.entity.internal.StudentGroupHistory;
import org.example.sapsanuserservice.entity.internal.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentGroupHistoryRepository extends JpaRepository<StudentGroupHistory,Long> {
    List<StudentGroupHistory> findAllByUserAndActiveTrue(User user);
    Boolean existsByUserAndGroup(User user, Group group);
}
