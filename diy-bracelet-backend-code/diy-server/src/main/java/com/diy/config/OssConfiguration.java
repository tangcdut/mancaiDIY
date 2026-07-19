package com.diy.config;

import com.diy.properties.MinioProperties;
import com.diy.utils.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;

@Configuration
@Slf4j
public class OssConfiguration {

    private final MinioProperties properties;

    public OssConfiguration(MinioProperties properties) {
        this.properties = properties;
    }

    /**
     * Real MinIO client — only when diy.minio.enabled is true (or unset, default true)
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "diy.minio.enabled", havingValue = "true", matchIfMissing = true)
    public MinioUtil minioUtil() {
        log.info("初始化 MinIO 对象存储客户端: endpoint={}, bucket={}",
                properties.getEndpoint(), properties.getBucketName());
        MinioUtil util = new MinioUtil(
                properties.getEndpoint(),
                properties.getAccessKey(),
                properties.getSecretKey(),
                properties.getBucketName()
        );
        try {
            util.ensureBucketExists();
        } catch (Exception e) {
            log.warn("MinIO 连接失败，继续启动（文件上传功能不可用）: {}", e.getMessage());
        }
        return util;
    }

    /**
     * Dummy MinIO bean when MinIO is disabled — allows app to start without a real MinIO server
     */
    @Bean
    @ConditionalOnProperty(name = "diy.minio.enabled", havingValue = "false")
    public MinioUtil minioUtilDummy() {
        log.warn("MinIO 已禁用（diy.minio.enabled=false），文件上传功能不可用");
        return new MinioUtil("http://localhost:9000", "dummy", "dummy", "dummy") {
            @Override
            public void ensureBucketExists() {
                // no-op
            }
        };
    }
}
