package com.diy.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "订单列表返回数据")
public class OrderListVO implements Serializable {

    @ApiModelProperty("订单列表")
    private List<OrderItem> orders;

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
    @ApiModel(description = "订单项")
    public static class OrderItem implements Serializable {
        
        @ApiModelProperty("订单ID")
        private Long orderId;
        
        @ApiModelProperty("订单编号")
        private String orderNo;
        
        @ApiModelProperty("订单金额")
        private BigDecimal amount;
        
        @ApiModelProperty("订单状态：0待支付 1已支付 2已发货 3已完成 4退款中 5已退款")
        private Integer status;
        
        @ApiModelProperty("订单状态文本")
        private String statusText;
        
        @ApiModelProperty("创建时间")
        private LocalDateTime createTime;
        
        @ApiModelProperty("支付时间")
        private LocalDateTime payTime;
        
        @ApiModelProperty("收货人姓名")
        private String receiverName;
        
        @ApiModelProperty("收货人电话")
        private String receiverPhone;
        
        @ApiModelProperty("收货地址（完整）")
        private String receiverAddress;

        @ApiModelProperty("商品图片URL（主图）")
        private String productImage;
        
        @ApiModelProperty("运单号（快递单号）")
        private String trackingNumber;
    }
}