package com.diy.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "商品详情返回数据")
public class ProductDetailVO implements Serializable {

    @ApiModelProperty("商品详情")
    private ProductItem product;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel(description = "商品项")
    public static class ProductItem implements Serializable {
        
        @ApiModelProperty("商品ID")
        private Long id;
        
        @ApiModelProperty("分类ID")
        private Long categoryId;
        
        @ApiModelProperty("分类名称")
        private String categoryName;
        
        @ApiModelProperty("商品标题")
        private String title;
        
        @ApiModelProperty("商品描述")
        private String description;
        
        @ApiModelProperty("商品封面图")
        private String coverImage;

        @ApiModelProperty("商品详情图列表（不包含封面，按顺序）")
        private List<String> detailImages;
        
        @ApiModelProperty("价格")
        private BigDecimal price;
        
        @ApiModelProperty("克重（单位：克）")
        private BigDecimal weight;
        
        @ApiModelProperty("库存")
        private Integer stock;
        
        @ApiModelProperty("状态：1上架 0下架")
        private Integer status;
    }
}