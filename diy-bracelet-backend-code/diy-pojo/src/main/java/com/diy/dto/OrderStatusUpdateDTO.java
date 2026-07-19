package com.diy.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderStatusUpdateDTO implements Serializable {

    //订单ID
    private Long orderId;

    //订单状态：1已支付 2已发货 3已完成
    private Integer status;
    
    //运单号（快递单号）
    private String trackingNumber;

}