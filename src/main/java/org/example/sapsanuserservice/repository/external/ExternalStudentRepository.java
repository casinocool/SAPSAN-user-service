package org.example.sapsanuserservice.repository.external;

import org.example.sapsanuserservice.entity.external.ExternalStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ExternalStudentRepository extends JpaRepository<ExternalStudentEntity,Long> {
    List<ExternalStudentEntity> findAllByGroupNumber(String groupNumber);

}
