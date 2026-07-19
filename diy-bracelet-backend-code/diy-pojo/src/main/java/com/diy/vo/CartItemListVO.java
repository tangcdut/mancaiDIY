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
@ApiModel(description = "购物车列表返回数据")
public class CartItemListVO implements Serializable {

    @ApiModelProperty("购物车项列表")
    private List<CartItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel(description = "购物车项")
    public static class CartItem implements Serializable {
        
        @ApiModelProperty("商品ID")
        private Long productId;
        
        @ApiModelProperty("商品标题")
        private String title;
        
        @ApiModelProperty("价格")
        private BigDecimal price;
        
        @ApiModelProperty("购买数量")
        private Integer quantity;
        
        @ApiModelProperty("商品图片URL")
        private String coverImage;
    }
}