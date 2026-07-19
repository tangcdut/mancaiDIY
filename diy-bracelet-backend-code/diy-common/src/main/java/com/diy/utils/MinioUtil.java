package com.diy.utils;

import io.minio.*;
import io.minio.http.Method;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class MinioUtil {

    private MinioClient minioClient;
    private String bucketName;
    private String endpoint;

    public MinioUtil(String endpoint, String accessKey, String secretKey, String bucketName) {
        this.bucketName = bucketName;
        this.endpoint = endpoint;
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                String policy = String.format(
                        "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::%s/*\"]}]}",
                        bucketName);
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build());
                log.info("MinIO 存储桶创建成功并设置公开读取策略: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("检查/创建 MinIO 存储桶失败", e);
            throw new RuntimeException("初始化 MinIO 存储桶失败: " + e.getMessage(), e);
        }
    }

    public String upload(byte[] bytes, String objectName) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(getContentType(objectName))
                    .build());
            String fileUrl = "/admin/common/image/" + objectName;
            log.info("文件上传成功: {}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    public byte[] download(String objectName) {
        try {
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] temp = new byte[4096];
            int len;
            while ((len = response.read(temp)) != -1) {
                buffer.write(temp, 0, len);
            }
            byte[] data = buffer.toByteArray();
            response.close();
            log.info("从 MinIO 下载文件成功: {}，大小={}字节", objectName, data.length);
            return data;
        } catch (Exception e) {
            log.error("从 MinIO 下载文件失败: {}", objectName, e);
            return null;
        }
    }

    public void delete(String fileUrlOrObjectName) {
        if (fileUrlOrObjectName == null || fileUrlOrObjectName.trim().isEmpty()) {
            return;
        }
        String objectName = fileUrlOrObjectName.trim();
        String prefix = "/admin/common/image/";
        if (objectName.startsWith(prefix)) {
            objectName = objectName.substring(prefix.length());
        }
        if (objectName.startsWith("/")) {
            objectName = objectName.substring(1);
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            log.info("从 MinIO 删除文件成功: {}", objectName);
        } catch (Exception e) {
            log.error("从 MinIO 删除文件失败: {}", objectName, e);
        }
    }

    public String getPresignedUrl(String objectName, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("生成预签名 URL 失败: {}", objectName, e);
            return null;
        }
    }

    private String getContentType(String objectName) {
        if (objectName == null) {
            return "application/octet-stream";
        }
        String lower = objectName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
