package org.example.sapsanuserservice.repository.internal;

import org.example.sapsanuserservice.entity.internal.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department,Long> {
    Optional<Department> findByNameIgnoreCase(String name);
    List<Department> findAllByNameInIgnoreCase(List<String> names);

}
