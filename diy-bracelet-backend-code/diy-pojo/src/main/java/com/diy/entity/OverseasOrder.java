package com.diy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 海外版订单实体（独立于国内订单表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverseasOrder implements Serializable {

    /** 订单状态 */
    public static final Integer PENDING_PAYMENT = 0;
    public static final Integer PAID = 1;
    public static final Integer SHIPPED = 2;
    public static final Integer COMPLETED = 3;
    public static final Integer CANCELLED = 4;

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 订单编号（OS + 时间戳） */
    private String orderNo;

    /** 订单金额 */
    private BigDecimal amount;

    /** 货币代码（USD等） */
    private String currency;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 详细地址 */
    private String receiverAddress;

    /** 城市 */
    private String receiverCity;

    /** 州/省 */
    private String receiverState;

    /** 邮政编码 */
    private String receiverZip;

    /** 国家 */
    private String receiverCountry;

    /** 状态：0待支付 1已支付 2已发货 3已完成 4已取消 */
    private Integer status;

    /** PayPal订单ID */
    private String paypalOrderId;

    /** PayPal捕获ID */
    private String paypalCaptureId;

    /** 订单备注 */
    private String remark;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
