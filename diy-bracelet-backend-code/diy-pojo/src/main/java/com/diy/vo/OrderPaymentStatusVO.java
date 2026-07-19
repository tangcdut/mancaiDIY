package com.diy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付状态VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentStatusVO implements Serializable {
    
    private Long orderId;           // 订单ID
    private String orderNo;         // 订单号
    private Integer status;         // 订单状态：1-待付款 2-已付款 3-已发货 4-已完成 5-已取消
    private BigDecimal amount;      // 订单金额
    private LocalDateTime createTime;   // 下单时间
    private LocalDateTime payTime;      // 支付时间
    private String transactionId;   // 微信支付交易号
    private Boolean isPaid;         // 是否已支付（便于前端判断）
    private String statusText;      // 状态文本（便于前端展示）
}
