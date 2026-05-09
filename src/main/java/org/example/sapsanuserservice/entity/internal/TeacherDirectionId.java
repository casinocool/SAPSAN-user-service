package org.example.sapsanuserservice.entity.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDirectionId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "direction_id")
    private Long directionId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeacherDirectionId that)) return false;
        return Objects.equals(userId, that.userId)
                && Objects.equals(directionId, that.directionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, directionId);
    }
}