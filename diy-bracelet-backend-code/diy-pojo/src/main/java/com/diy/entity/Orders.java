package com.diy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orders implements Serializable {

    /**
     * 订单状态 0待支付 1已支付 2已发货 3已完成 4退款中 5已退款 6已取消
     */
    public static final Integer PENDING_PAYMENT = 0;
    public static final Integer PAID = 1;
    public static final Integer SHIPPED = 2;
    public static final Integer COMPLETED = 3;
    public static final Integer REFUNDING = 4;
    public static final Integer REFUNDED = 5;
    public static final Integer CANCELLED = 6;

    private static final long serialVersionUID = 1L;

    private Long id;

    //用户ID
    private Long userId;

    //订单编号
    private String orderNo;

    //订单金额
    private BigDecimal amount;

    //收货人姓名
    private String receiverName;

    //收货人电话
    private String receiverPhone;

    //联系邮箱（仅海外免登录订单有值）
    private String email;

    //收货省份
    private String receiverProvince;

    //收货城市
    private String receiverCity;

    //收货区县
    private String receiverDistrict;

    //详细地址
    private String receiverDetail;

    //状态：0待支付 1已支付 2已发货 3已完成 4退款中 5已退款 6已取消
    private Integer status;

    //支付时间
    private LocalDateTime payTime;

    //发货时间
    private LocalDateTime deliveryTime;

    //完成时间
    private LocalDateTime completeTime;

    //下单时间
    private LocalDateTime createTime;

    //更新时间
    private LocalDateTime updateTime;

    //微信支付交易号
    private String transactionId;
    
    //微信预支付交易会话ID
    private String prepayId;

    //退款ID（微信退款单号）
    private String refundId;

    //退款金额
    private BigDecimal refundAmount;

    //退款时间
    private LocalDateTime refundTime;

    //退款原因
    private String refundReason;

    //订单备注
    private String remark;
    
    //DIY作品名称（仅DIY订单有值）
    private String diyName;
    
    //手链组成描述（如材料顺序等）
    private String description;
    
    //商品图片URL（主图）
    private String productImage;
    
    //运单号（快递单号）
    private String trackingNumber;

    //是否海外订单：0国内版，1海外版
    private Integer isOverseas;

    //PayPal订单ID
    private String paypalOrderId;

    //PayPal捕获ID
    private String paypalCaptureId;
}