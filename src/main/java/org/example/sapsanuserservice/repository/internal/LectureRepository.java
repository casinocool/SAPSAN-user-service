package org.example.sapsanuserservice.repository.internal;

import org.example.sapsanuserservice.entity.internal.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {

    /**
     * Лекции направлений, в которых состоит пользователь
     * (для студента — через активные группы, для учителя — через teacher_direction).
     */
    List<Lecture> findAllByDepartmentIdInOrderByCreatedAtDesc(List<Long> departmentId);

    /**
     * Направления, к которым относится активный студент через свои группы.
     */
    @Query("""
        SELECT DISTINCT g.department.id
        FROM StudentGroupHistory h
        JOIN h.group g
        WHERE h.user.id = :userId AND h.active = true
        """)
    List<Long> findDepartmentIdsForStudent(@Param("userId") UUID userId);
}
