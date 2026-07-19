package com.diy.controller.admin;

import com.diy.constant.MessageConstant;
import com.diy.entity.Banner;
import com.diy.result.Result;
import com.diy.service.BannerService;
import com.diy.utils.MinioUtil;
import com.diy.vo.BannerVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 管理端 - 轮播图管理接口
 */
@RestController("adminBannerController")
@RequestMapping("/admin/banner")
@Api(tags = "管理端-轮播图接口")
@Slf4j
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @Autowired
    private MinioUtil minioUtil;

    /**
     * 获取轮播图列表
     */
    @GetMapping
    @ApiOperation("获取轮播图列表")
    public Result<BannerVO> list() {
        log.info("管理端查询轮播图列表");
        List<Banner> banners = bannerService.list();
        List<BannerVO.BannerItem> items = banners.stream().map(banner -> {
            BannerVO.BannerItem item = new BannerVO.BannerItem();
            BeanUtils.copyProperties(banner, item);
            return item;
        }).collect(Collectors.toList());
        BannerVO bannerVO = BannerVO.builder()
                .banners(items)
                .build();
        return Result.success(bannerVO);
    }

    /**
     * 新增轮播图
     */
    @PostMapping("/add")
    @ApiOperation("新增轮播图")
    public Result<String> add(@RequestBody Banner banner) {
        log.info("新增轮播图: {}", banner);
        bannerService.add(banner);
        return Result.success();
    }

    /**
     * 更新轮播图
     */
    @PostMapping("/update")
    @ApiOperation("更新轮播图")
    public Result<String> update(@RequestBody Banner banner) {
        log.info("更新轮播图: {}", banner);
        bannerService.update(banner);
        return Result.success();
    }

    /**
     * 删除轮播图
     */
    @DeleteMapping("/delete/{id}")
    @ApiOperation("删除轮播图")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除轮播图: id={}", id);
        bannerService.delete(id);
        return Result.success();
    }

    /**
     * 上传轮播图图片
     * @param file 图片文件
     * @return 图片访问路径
     */
    @PostMapping("/upload")
    @ApiOperation("上传轮播图图片")
    public Result<String> uploadBannerImage(@RequestParam("file") MultipartFile file) {
        log.info("上传轮播图图片: {}", file.getOriginalFilename());

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.error("文件名不能为空");
            }

            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + extension;
            String objectName = "banners/" + fileName;

            String fileUrl = minioUtil.upload(file.getBytes(), objectName);
            log.info("轮播图图片上传成功: {}", fileUrl);
            return Result.success(fileUrl);
        } catch (Exception e) {
            log.error("轮播图图片上传失败", e);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
    }
}
