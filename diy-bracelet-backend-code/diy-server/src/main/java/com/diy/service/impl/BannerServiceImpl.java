package com.diy.service.impl;

import com.diy.entity.Banner;
import com.diy.mapper.BannerMapper;
import com.diy.service.BannerService;
import com.diy.utils.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BannerServiceImpl implements BannerService {

    @Autowired
    private BannerMapper bannerMapper;

    @Autowired
    private MinioUtil minioUtil;

    /**
     * 查询轮播图列表
     */
    @Override
    public List<Banner> list() {
        log.info("查询轮播图列表");
        return bannerMapper.list();
    }

    @Override
    public void add(Banner banner) {
        log.info("新增轮播图: {}", banner);
        bannerMapper.insert(banner);
    }

    @Override
    public void update(Banner banner) {
        log.info("更新轮播图: {}", banner);
        if (banner.getId() != null) {
            Banner old = bannerMapper.getById(banner.getId());
            if (old != null) {
                String oldImage = old.getImageUrl();
                String newImage = banner.getImageUrl();
                if (oldImage != null && !oldImage.isEmpty()
                        && newImage != null && !newImage.isEmpty()
                        && !oldImage.equals(newImage)) {
                    log.info("删除旧轮播图图片: {}", oldImage);
                    minioUtil.delete(oldImage);
                }
            }
        }
        bannerMapper.update(banner);
    }

    @Override
    public void delete(Long id) {
        log.info("删除轮播图: id={}", id);
        Banner old = bannerMapper.getById(id);
        if (old != null && old.getImageUrl() != null && !old.getImageUrl().isEmpty()) {
            log.info("删除轮播图图片文件: {}", old.getImageUrl());
            minioUtil.delete(old.getImageUrl());
        }
        bannerMapper.delete(id);
    }
}
