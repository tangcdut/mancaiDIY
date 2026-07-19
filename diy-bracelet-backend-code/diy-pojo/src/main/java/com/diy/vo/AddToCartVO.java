package com.diy.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "添加购物车返回数据")
public class AddToCartVO implements Serializable {

    @ApiModelProperty("购物车项")
    private CartItem cartItem;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel(description = "购物车项")
    public static class CartItem implements Serializable {
        
        @ApiModelProperty("商品ID")
        private Long productId;
        
        @ApiModelProperty("购买数量")
        private Integer quantity;
    }
}