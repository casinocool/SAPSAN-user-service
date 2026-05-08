package org.example.sapsanuserservice.entity.external;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "ext_university_data")
@AllArgsConstructor
@NoArgsConstructor
public class ExternalStudentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="full_name")
    private String fullName;

    @Column(unique = true)
    private String email;

    @Column(name = "group_number")
    private String groupNumber;

    @Column(name = "department_name")
    private String departmentName;

    @Column(name = "direction_name")
    private String directionName;

    @Column(name = "institute_name")
    private String instituteName;

}
