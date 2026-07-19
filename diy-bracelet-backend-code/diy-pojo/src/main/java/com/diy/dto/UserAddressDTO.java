package com.diy.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 用户地址DTO（添加和更新）
 */
@Data
@ApiModel(description = "用户地址信息")
public class UserAddressDTO implements Serializable {

    @ApiModelProperty("地址ID（更新时需要）")
    private Long id;

    @NotBlank(message = "收货人姓名不能为空")
    @ApiModelProperty(value = "收货人姓名", required = true)
    private String consignee;

    @NotBlank(message = "手机号不能为空")
    @ApiModelProperty(value = "手机号", required = true)
    private String phone;

    @ApiModelProperty(value = "省份", required = false)
    private String province;

    @NotBlank(message = "城市不能为空")
    @ApiModelProperty(value = "城市", required = true)
    private String city;

    @ApiModelProperty(value = "区/县", required = false)
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @ApiModelProperty(value = "详细地址", required = true)
    private String detailAddress;

    @NotNull(message = "是否默认地址不能为空")
    @ApiModelProperty(value = "是否默认地址：0-否，1-是", required = true)
    private Integer isDefault;
}
