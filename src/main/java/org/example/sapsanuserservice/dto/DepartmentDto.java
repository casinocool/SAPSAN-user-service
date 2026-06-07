package org.example.sapsanuserservice.dto;

import org.example.sapsanuserservice.entity.internal.Department;

public record DepartmentDto(Long id, String name) {
    public static DepartmentDto from(Department d) {
        return new DepartmentDto(d.getId(), d.getName());
    }
}
