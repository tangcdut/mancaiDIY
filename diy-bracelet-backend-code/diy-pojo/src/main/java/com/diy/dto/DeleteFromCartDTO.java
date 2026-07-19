package com.diy.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "删除购物车商品DTO")
public class DeleteFromCartDTO implements Serializable {

    @ApiModelProperty("商品ID")
    private Long productId;
}