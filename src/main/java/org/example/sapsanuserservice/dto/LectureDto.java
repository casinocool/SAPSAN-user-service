package org.example.sapsanuserservice.dto;

import org.example.sapsanuserservice.entity.internal.Lecture;

import java.time.LocalDateTime;

public record LectureDto(
        Long id,
        String title,
        String originalFileName,
        String contentType,
        Long sizeBytes,
        Long departmentId,
        String departmentName,
        String uploaderName,
        String uploaderEmail,
        LocalDateTime createdAt
) {
    public static LectureDto from(Lecture l) {
        var u = l.getUploader();
        String name = ((u.getLastName() != null ? u.getLastName() : "")
                + " "
                + (u.getFirstName() != null ? u.getFirstName() : "")).trim();
        return new LectureDto(
                l.getId(),
                l.getTitle(),
                l.getOriginalFileName(),
                l.getContentType(),
                l.getSizeBytes(),
                l.getDepartment().getId(),
                l.getDepartment().getName(),
                name.isBlank() ? u.getEmail() : name,
                u.getEmail(),
                l.getCreatedAt()
        );
    }
}
