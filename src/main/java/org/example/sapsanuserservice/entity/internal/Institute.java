package org.example.sapsanuserservice.entity.internal;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "institutes")
@Data
public class Institute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
}
