package com.diy.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentVO implements Serializable {

    private String nonceStr; //随机字符串
    private String paySign; //签名
    private String timeStamp; //时间戳
    private String signType; //签名算法
    private String packageStr; //统一下单接口返回的 prepay_id 参数值
    
    // PayPal支付相关字段
    private String paypalOrderId; // PayPal订单ID
    private String paypalApprovalUrl; // PayPal支付审批链接
    private String approvalUrl; // 兼容前端字段：PayPal支付审批链接
    private String paymentMethod; // 支付方式：WECHAT或PAYPAL

}
