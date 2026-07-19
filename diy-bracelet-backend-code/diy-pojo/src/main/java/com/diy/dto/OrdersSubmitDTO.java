package com.diy.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "用户下单DTO")
public class OrdersSubmitDTO implements Serializable {
    
    // 收货地址ID
    private Long addressId;
    
    // 备注（可选）
    private String remark;
    
    // 收货人姓名（非必填，用于直接传递收件人信息）
    private String receiverName;
    
    // 收货人电话（非必填，用于直接传递收件人信息）
    private String receiverPhone;
    
    // 收货省份（非必填，用于直接传递收件人信息）
    private String receiverProvince;
    
    // 收货城市（非必填，用于直接传递收件人信息）
    private String receiverCity;
    
    // 收货区县（非必填，用于直接传递收件人信息）
    private String receiverDistrict;
    
    // 详细地址（非必填，用于直接传递收件人信息）
    private String receiverDetail;

    //是否海外订单：0否，1是
    private Integer isOverseas;
}