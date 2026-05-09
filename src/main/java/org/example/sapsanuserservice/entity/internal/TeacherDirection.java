package org.example.sapsanuserservice.entity.internal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teacher_direction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherDirection {

    @EmbeddedId
    private TeacherDirectionId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("directionId")
    @JoinColumn(name = "direction_id", nullable = false)
    private Direction direction;
}