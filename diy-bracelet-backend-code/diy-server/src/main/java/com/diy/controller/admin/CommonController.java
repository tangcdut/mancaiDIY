package com.diy.controller.admin;

import com.diy.constant.MessageConstant;
import com.diy.entity.CustomerServiceQr;
import com.diy.mapper.CustomerServiceQrMapper;
import com.diy.result.Result;
import com.diy.utils.MinioUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@Api(tags = "通用接口")
@Slf4j
public class CommonController {
    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private CustomerServiceQrMapper customerServiceQrMapper;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");

    @ApiOperation("文件上传")
    @PostMapping("/admin/common/upload")
    public Result<String> upload(MultipartFile file,
                                 @RequestParam(value = "type", required = false, defaultValue = "other") String type) {
        log.info("文件上传: {}, 类型: {}", file, type);
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.error("文件名不能为空");
            }

            String folder;
            switch (type.toLowerCase()) {
                case "product":
                    folder = "products";
                    break;
                case "banner":
                    folder = "banners";
                    break;
                case "diy":
                case "diy_design":
                    folder = "diy_designs";
                    break;
                case "customer_service":
                case "qr":
                    folder = "customer_service";
                    break;
                default:
                    folder = "others";
                    break;
            }

            String extension = getFileExtension(originalFilename);
            String fileName = folder + "/" + UUID.randomUUID().toString() + "." + extension;
            String filepath = minioUtil.upload(file.getBytes(), fileName);
            return Result.success(filepath);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
    }

    @ApiOperation("用户端/游客上传DIY截图")
    @PostMapping({"/user/common/upload", "/guest/common/upload"})
    public Result<java.util.Map<String, String>> uploadDiyScreenshot(@RequestParam("file") MultipartFile file,
                                                                     @RequestParam(value = "scene", required = false) String scene) {
        log.info("用户端/游客上传DIY截图: file={}, scene={}", file != null ? file.getOriginalFilename() : null, scene);
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        // 该接口对游客开放（/guest/**），限制文件大小防止滥用
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error("文件大小不能超过10MB");
        }
        if (scene == null || scene.trim().isEmpty()) {
            scene = "diy_design";
        }
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String folder = "diy_designs";
            String objectName = folder + "/" + scene + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
            String path = minioUtil.upload(file.getBytes(), objectName);
            java.util.Map<String, String> result = new java.util.HashMap<>();
            result.put("path", path);
            result.put("url", path);
            return Result.success(result);
        } catch (Exception e) {
            log.error("用户端上传DIY截图失败", e);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
    }

    @ApiOperation("读取图片文件")
    @GetMapping("/admin/common/image/**")
    public ResponseEntity<Resource> getImage(HttpServletRequest request) {
        try {
            String requestURI = request.getRequestURI();
            String imagePath = requestURI.substring(requestURI.indexOf("/admin/common/image/") + 20);

            log.info("读取图片文件: {}", imagePath);

            String cleanPath = imagePath;
            if (cleanPath.startsWith("/") || cleanPath.startsWith("\\")) {
                cleanPath = cleanPath.substring(1);
            }
            cleanPath = cleanPath.replace("\\", "/");

            if (cleanPath.contains("../") || cleanPath.contains("..\\")) {
                log.warn("检测到路径遍历攻击尝试: {}", imagePath);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            byte[] data = minioUtil.download(cleanPath);
            if (data == null || data.length == 0) {
                log.warn("MinIO 中未找到文件: {}", cleanPath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String extension = getFileExtension(cleanPath).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                log.warn("不支持的文件类型: {}", extension);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            MediaType mediaType = getMediaType(extension);
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
                    .body(resource);

        } catch (Exception e) {
            log.error("读取图片文件失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private MediaType getMediaType(String extension) {
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            case "gif":
                return MediaType.IMAGE_GIF;
            case "bmp":
                return MediaType.valueOf("image/bmp");
            case "webp":
                return MediaType.valueOf("image/webp");
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @ApiOperation("上传客服二维码")
    @PostMapping("/admin/common/upload/customer-service-qr")
    public Result<String> uploadCustomerServiceQR(@RequestParam("file") MultipartFile file) {
        log.info("上传客服二维码: {}", file.getOriginalFilename());

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.error("文件名不能为空");
            }

            String extension = getFileExtension(originalFilename).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                return Result.error("只支持图片格式: " + String.join(", ", ALLOWED_EXTENSIONS));
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error("文件大小不能超过5MB");
            }

            CustomerServiceQr oldRecord = customerServiceQrMapper.getActiveOne();
            if (oldRecord != null && oldRecord.getImagePath() != null && !oldRecord.getImagePath().isEmpty()) {
                log.info("准备删除旧客服二维码图片: {}", oldRecord.getImagePath());
                minioUtil.delete(oldRecord.getImagePath());
            }

            String objectName = "customer_service/qr_code_" + System.currentTimeMillis() + "." + extension;

            String cloudPath = minioUtil.upload(file.getBytes(), objectName);
            log.info("客服二维码上传成功到 MinIO: {}", cloudPath);

            customerServiceQrMapper.disableAll();

            CustomerServiceQr record = CustomerServiceQr.builder()
                    .imagePath(cloudPath)
                    .status(1)
                    .build();
            customerServiceQrMapper.insert(record);

            return Result.success(cloudPath);

        } catch (Exception e) {
            log.error("客服二维码上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @ApiOperation("获取客服二维码 - 管理端")
    @GetMapping("/admin/common/customer-service-qr")
    public Result<String> getCustomerServiceQR() {
        try {
            CustomerServiceQr record = customerServiceQrMapper.getActiveOne();
            if (record == null) {
                return Result.error("客服二维码未上传");
            }
            log.info("获取客服二维码: {}", record.getImagePath());
            return Result.success(record.getImagePath());
        } catch (Exception e) {
            log.error("获取客服二维码失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    @ApiOperation("删除客服二维码")
    @DeleteMapping("/admin/common/customer-service-qr")
    public Result<String> deleteCustomerServiceQR() {
        try {
            CustomerServiceQr record = customerServiceQrMapper.getActiveOne();
            if (record != null && record.getImagePath() != null && !record.getImagePath().isEmpty()) {
                log.info("删除客服二维码图片文件: {}", record.getImagePath());
                minioUtil.delete(record.getImagePath());
            }

            customerServiceQrMapper.disableAll();
            log.info("已标记所有客服二维码记录为禁用");
            return Result.success("删除成功");

        } catch (Exception e) {
            log.error("删除客服二维码失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @ApiOperation("获取客服二维码 - 用户端")
    @GetMapping("/user/common/customer-service-qr")
    public Result<String> getUserCustomerServiceQR() {
        try {
            CustomerServiceQr record = customerServiceQrMapper.getActiveOne();
            if (record == null) {
                return Result.error("客服二维码未上传");
            }
            log.info("[用户端]获取客服二维码: {}", record.getImagePath());
            return Result.success(record.getImagePath());

        } catch (Exception e) {
            log.error("[用户端]获取客服二维码失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}
