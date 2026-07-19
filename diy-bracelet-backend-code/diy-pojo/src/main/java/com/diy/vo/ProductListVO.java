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
@ApiModel(description = "商品列表返回数据")
public class ProductListVO implements Serializable {

    @ApiModelProperty("商品列表")
    private List<ProductItem> products;

    @ApiModelProperty("当前页码")
    private Integer page;

    @ApiModelProperty("每页大小")
    private Integer size;

    @ApiModelProperty("总记录数")
    private Long total;

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
        
        @ApiModelProperty("商品封面图")
        private String coverImage;
        
        @ApiModelProperty("价格")
        private BigDecimal price;

        @ApiModelProperty("海外价格")
        private BigDecimal priceOverseas;
        
        @ApiModelProperty("库存")
        private Integer stock;
        
        @ApiModelProperty("状态：1上架 0下架")
        private Integer status;
    }
}