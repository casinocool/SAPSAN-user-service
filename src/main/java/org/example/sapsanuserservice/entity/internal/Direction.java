package org.example.sapsanuserservice.entity.internal;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "directions")
@Data
public class Direction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institute_id")
    private Institute institute;
}