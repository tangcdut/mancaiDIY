package com.diy.controller.user;

import com.diy.entity.Banner;
import com.diy.result.Result;
import com.diy.service.BannerService;
import com.diy.vo.BannerVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user/banner")
@Api(tags = "轮播图接口")
@Slf4j
public class BannerController {

    @Autowired
    private BannerService bannerService;

    /**
     * 查询轮播图列表
     */
    @GetMapping("/list")
    @ApiOperation("查询轮播图列表")
    public Result<BannerVO> list() {
        log.info("查询轮播图列表");
        
        List<Banner> banners = bannerService.list();
        
        // 转换为VO
        List<BannerVO.BannerItem> bannerItems = banners.stream().map(banner -> {
            BannerVO.BannerItem item = new BannerVO.BannerItem();
            BeanUtils.copyProperties(banner, item);
            return item;
        }).collect(Collectors.toList());
        
        BannerVO bannerVO = BannerVO.builder()
                .banners(bannerItems)
                .build();
        
        return Result.success(bannerVO);
    }
}
