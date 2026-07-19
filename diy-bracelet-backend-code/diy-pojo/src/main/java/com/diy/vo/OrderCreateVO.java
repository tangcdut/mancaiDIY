package com.diy.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "创建订单返回数据")
public class OrderCreateVO implements Serializable {

    @ApiModelProperty("订单信息")
    private Order order;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel(description = "订单")
    public static class Order implements Serializable {
        
        @ApiModelProperty("订单ID")
        private Long orderId;
        
        @ApiModelProperty("订单编号")
        private String orderNo;
        
        @ApiModelProperty("订单金额")
        private BigDecimal amount;
        
        @ApiModelProperty("订单状态：0待支付 1已支付 2已发货 3已完成")
        private Integer status;
    }
}