package org.example.sapsanuserservice.repository.internal;

import org.example.sapsanuserservice.entity.internal.TeacherDirection;
import org.example.sapsanuserservice.entity.internal.TeacherDirectionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherDirectionRepository extends JpaRepository<TeacherDirection, TeacherDirectionId> {
    
}
