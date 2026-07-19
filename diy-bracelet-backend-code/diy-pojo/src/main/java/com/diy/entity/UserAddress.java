package com.diy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户地址
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    // 主键ID
    private Long id;

    // 用户ID
    private Long userId;

    // 收货人姓名
    private String consignee;

    // 手机号
    private String phone;

    // 省份
    private String province;

    // 城市
    private String city;

    // 区/县
    private String district;

    // 详细地址
    private String detailAddress;

    // 是否默认地址：0-否，1-是
    private Integer isDefault;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
