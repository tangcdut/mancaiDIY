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
@ApiModel(description = "订单详情返回数据")
public class OrderDetailVO implements Serializable {

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
        
        @ApiModelProperty("订单状态：0待支付 1已支付 2已发货 3已完成 4退款中 5已退款")
        private Integer status;
        
        @ApiModelProperty("创建时间")
        private LocalDateTime createTime;
        
        @ApiModelProperty("支付时间")
        private LocalDateTime payTime;
        
        @ApiModelProperty("发货时间")
        private LocalDateTime deliveryTime;
        
        @ApiModelProperty("完成时间")
        private LocalDateTime completeTime;
        
        @ApiModelProperty("收货人姓名")
        private String receiverName;
        
        @ApiModelProperty("收货人电话")
        private String receiverPhone;
        
        @ApiModelProperty("收货省份")
        private String receiverProvince;
        
        @ApiModelProperty("收货城市")
        private String receiverCity;
        
        @ApiModelProperty("收货区县")
        private String receiverDistrict;
        
        @ApiModelProperty("详细地址")
        private String receiverDetail;
        
        @ApiModelProperty("微信支付交易号")
        private String transactionId;
        
        @ApiModelProperty("退款金额")
        private BigDecimal refundAmount;
        
        @ApiModelProperty("退款时间")
        private LocalDateTime refundTime;
        
        @ApiModelProperty("退款原因")
        private String refundReason;
        
        @ApiModelProperty("微信退款单号")
        private String refundId;
        
        @ApiModelProperty("订单备注")
        private String remark;
        
        @ApiModelProperty("商品图片URL（订单主图）")
        private String productImage;
        
        @ApiModelProperty("手链组成描述（如材料顺序等）")
        private String description;
        
        @ApiModelProperty("运单号（快递单号）")
        private String trackingNumber;
        
        @ApiModelProperty("订单项列表")
        private List<Item> items;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel(description = "订单项")
    public static class Item implements Serializable {
        
        @ApiModelProperty("商品ID")
        private Long productId;
        
        @ApiModelProperty("商品标题")
        private String title;
        
        @ApiModelProperty("单价")
        private BigDecimal price;
        
        @ApiModelProperty("数量")
        private Integer quantity;
        
        @ApiModelProperty("商品图片URL")
        private String productImage;
    }
}