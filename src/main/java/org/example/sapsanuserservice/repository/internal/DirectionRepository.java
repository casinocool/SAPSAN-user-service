package org.example.sapsanuserservice.repository.internal;

import org.example.sapsanuserservice.entity.internal.Direction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectionRepository extends JpaRepository<Direction, Long> {

    Optional<Direction> findByNameIgnoreCase(String name);

    /** Найти все направления по списку имён (как они приходят в JWT-claim). */
    List<Direction> findAllByNameInIgnoreCase(List<String> names);
}
