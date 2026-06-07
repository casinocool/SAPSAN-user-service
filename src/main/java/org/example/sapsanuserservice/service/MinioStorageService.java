package org.example.sapsanuserservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final S3Client s3Client;

    @Value("${minio.bucket}")
    private String bucket;

    /** Загружает файл в bucket. Возвращает сгенерированный ключ. */
    public String upload(MultipartFile file) throws IOException {
        String key = generateKey(file.getOriginalFilename());

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(req,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        log.info("Uploaded: bucket={}, key={}, size={}", bucket, key, file.getSize());
        return key;
    }

    /** Открывает InputStream объекта; закрывает вызывающий код. */
    public ResponseInputStream<GetObjectResponse> download(String key) {
        return s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket).key(key).build());
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket).key(key).build());
        log.info("Deleted: bucket={}, key={}", bucket, key);
    }

    private String generateKey(String originalName) {
        String ext = "";
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot > -1 && dot < originalName.length() - 1) {
                ext = originalName.substring(dot);
            }
        }
        return "lectures/" + UUID.randomUUID() + ext;
    }
}
