package com.diy.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "轮播图列表返回数据")
public class BannerVO implements Serializable {

    @ApiModelProperty("轮播图列表")
    private List<BannerItem> banners;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel(description = "轮播图项")
    public static class BannerItem implements Serializable {
        
        @ApiModelProperty("轮播图ID")
        private Long id;
        
        @ApiModelProperty("图片地址")
        private String imageUrl;
        
        @ApiModelProperty("跳转链接")
        private String link;
        
        @ApiModelProperty("排序")
        private Integer sort;
    }
}
