package com.diy.service;

import com.diy.entity.Banner;

import java.util.List;

public interface BannerService {
    
    /**
     * 查询轮播图列表
     */
    List<Banner> list();

    /**
     * 新增轮播图
     * @param banner 轮播图信息
     */
    void add(Banner banner);

    /**
     * 更新轮播图
     * @param banner 轮播图信息
     */
    void update(Banner banner);

    /**
     * 删除轮播图
     * @param id 轮播图ID
     */
    void delete(Long id);
}
