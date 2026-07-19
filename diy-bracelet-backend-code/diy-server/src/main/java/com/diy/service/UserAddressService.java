package com.diy.service;

import com.diy.dto.UserAddressDTO;
import com.diy.entity.UserAddress;
import com.diy.vo.UserAddressVO;

import java.util.List;

public interface UserAddressService {

    /**
     * 添加地址
     * @param userAddressDTO 地址信息
     * @return 添加后的地址信息
     */
    UserAddressVO addAddress(UserAddressDTO userAddressDTO);

    /**
     * 更新地址
     * @param userAddressDTO 地址信息
     * @return 更新后的地址信息
     */
    UserAddressVO updateAddress(UserAddressDTO userAddressDTO);

    /**
     * 删除地址
     * @param id 地址ID
     */
    void deleteAddress(Long id);

    /**
     * 查询用户的所有地址
     * @return 地址列表
     */
    List<UserAddressVO> listAddress();

    /**
     * 根据ID查询地址
     * @param id 地址ID
     * @return 地址信息
     */
    UserAddressVO getAddressById(Long id);

    /**
     * 设置默认地址
     * @param id 地址ID
     */
    void setDefaultAddress(Long id);

    /**
     * 获取默认地址
     * @return 默认地址
     */
    UserAddressVO getDefaultAddress();
}
