package com.diy.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "添加购物车DTO")
public class AddToCartDTO implements Serializable {

    @ApiModelProperty("商品ID")
    private Long productId;

    @ApiModelProperty("购买数量")
    private Integer quantity;
}