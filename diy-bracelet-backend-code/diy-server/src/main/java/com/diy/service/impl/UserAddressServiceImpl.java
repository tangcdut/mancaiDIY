package com.diy.service.impl;

import com.diy.context.BaseContext;
import com.diy.dto.UserAddressDTO;
import com.diy.entity.UserAddress;
import com.diy.mapper.UserAddressMapper;
import com.diy.service.UserAddressService;
import com.diy.vo.UserAddressVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserAddressServiceImpl implements UserAddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    /**
     * 添加地址
     * @param userAddressDTO 地址信息
     * @return 添加后的地址信息
     */
    @Override
    @Transactional
    public UserAddressVO addAddress(UserAddressDTO userAddressDTO) {
        Long userId = BaseContext.getCurrentId();
        log.info("添加地址，用户ID: {}, 地址信息: {}", userId, userAddressDTO);

        // 参数校验
        validateAddressDTO(userAddressDTO, false);

        // 如果设置为默认地址，先将该用户的其他地址设置为非默认
        if (userAddressDTO.getIsDefault() != null && userAddressDTO.getIsDefault() == 1) {
            userAddressMapper.clearDefaultByUserId(userId);
        }

        // 构建UserAddress对象
        UserAddress userAddress = UserAddress.builder()
                .userId(userId)
                .consignee(userAddressDTO.getConsignee())
                .phone(userAddressDTO.getPhone())
                .province(userAddressDTO.getProvince())
                .city(userAddressDTO.getCity())
                .district(userAddressDTO.getDistrict())
                .detailAddress(userAddressDTO.getDetailAddress())
                .isDefault(userAddressDTO.getIsDefault())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 插入数据库
        userAddressMapper.insert(userAddress);

        // 转换为VO返回
        return convertToVO(userAddress);
    }

    /**
     * 更新地址
     * @param userAddressDTO 地址信息
     * @return 更新后的地址信息
     */
    @Override
    @Transactional
    public UserAddressVO updateAddress(UserAddressDTO userAddressDTO) {
        Long userId = BaseContext.getCurrentId();
        log.info("更新地址，用户ID: {}, 地址信息: {}", userId, userAddressDTO);

        // 参数校验
        validateAddressDTO(userAddressDTO, true);

        // 查询原地址信息，验证是否属于当前用户
        UserAddress existingAddress = userAddressMapper.getById(userAddressDTO.getId());
        if (existingAddress == null || !existingAddress.getUserId().equals(userId)) {
            throw new RuntimeException("地址不存在或无权限操作");
        }

        // 如果设置为默认地址，先将该用户的其他地址设置为非默认
        if (userAddressDTO.getIsDefault() != null && userAddressDTO.getIsDefault() == 1) {
            userAddressMapper.clearDefaultByUserId(userId);
        }

        // 构建更新对象
        UserAddress userAddress = UserAddress.builder()
                .id(userAddressDTO.getId())
                .consignee(userAddressDTO.getConsignee())
                .phone(userAddressDTO.getPhone())
                .province(userAddressDTO.getProvince())
                .city(userAddressDTO.getCity())
                .district(userAddressDTO.getDistrict())
                .detailAddress(userAddressDTO.getDetailAddress())
                .isDefault(userAddressDTO.getIsDefault())
                .updateTime(LocalDateTime.now())
                .build();

        // 更新数据库
        userAddressMapper.update(userAddress);

        // 查询更新后的地址
        UserAddress updatedAddress = userAddressMapper.getById(userAddressDTO.getId());
        return convertToVO(updatedAddress);
    }

    /**
     * 删除地址
     * @param id 地址ID
     */
    @Override
    public void deleteAddress(Long id) {
        Long userId = BaseContext.getCurrentId();
        log.info("删除地址，用户ID: {}, 地址ID: {}", userId, id);

        // 查询地址信息，验证是否属于当前用户
        UserAddress existingAddress = userAddressMapper.getById(id);
        if (existingAddress == null || !existingAddress.getUserId().equals(userId)) {
            throw new RuntimeException("地址不存在或无权限操作");
        }

        // 删除地址
        userAddressMapper.deleteById(id);
    }

    /**
     * 查询用户的所有地址
     * @return 地址列表
     */
    @Override
    public List<UserAddressVO> listAddress() {
        Long userId = BaseContext.getCurrentId();
        log.info("查询用户地址列表，用户ID: {}", userId);

        List<UserAddress> addressList = userAddressMapper.listByUserId(userId);
        return addressList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID查询地址
     * @param id 地址ID
     * @return 地址信息
     */
    @Override
    public UserAddressVO getAddressById(Long id) {
        Long userId = BaseContext.getCurrentId();
        log.info("查询地址详情，用户ID: {}, 地址ID: {}", userId, id);

        UserAddress userAddress = userAddressMapper.getById(id);
        if (userAddress == null || !userAddress.getUserId().equals(userId)) {
            throw new RuntimeException("地址不存在或无权限访问");
        }

        return convertToVO(userAddress);
    }

    /**
     * 设置默认地址
     * @param id 地址ID
     */
    @Override
    @Transactional
    public void setDefaultAddress(Long id) {
        Long userId = BaseContext.getCurrentId();
        log.info("设置默认地址，用户ID: {}, 地址ID: {}", userId, id);

        // 查询地址信息，验证是否属于当前用户
        UserAddress existingAddress = userAddressMapper.getById(id);
        if (existingAddress == null || !existingAddress.getUserId().equals(userId)) {
            throw new RuntimeException("地址不存在或无权限操作");
        }

        // 先将该用户的所有地址设置为非默认
        userAddressMapper.clearDefaultByUserId(userId);

        // 将指定地址设置为默认
        UserAddress userAddress = UserAddress.builder()
                .id(id)
                .isDefault(1)
                .updateTime(LocalDateTime.now())
                .build();
        userAddressMapper.update(userAddress);
    }

    /**
     * 获取默认地址
     * @return 默认地址
     */
    @Override
    public UserAddressVO getDefaultAddress() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取默认地址，用户ID: {}", userId);

        UserAddress defaultAddress = userAddressMapper.getDefaultByUserId(userId);
        if (defaultAddress == null) {
            return null;
        }

        return convertToVO(defaultAddress);
    }

    /**
     * 将UserAddress转换为UserAddressVO
     * @param userAddress 用户地址实体
     * @return 用户地址VO
     */
    private UserAddressVO convertToVO(UserAddress userAddress) {
        UserAddressVO vo = new UserAddressVO();
        BeanUtils.copyProperties(userAddress, vo);
        return vo;
    }

    /**
     * 校验地址DTO参数
     * @param userAddressDTO 地址DTO
     * @param isUpdate 是否为更新操作
     */
    private void validateAddressDTO(UserAddressDTO userAddressDTO, boolean isUpdate) {
        // 更新操作时，ID不能为空
        if (isUpdate && userAddressDTO.getId() == null) {
            throw new RuntimeException("地址ID不能为空");
        }

        // 收货人姓名不能为空
        if (userAddressDTO.getConsignee() == null || userAddressDTO.getConsignee().trim().isEmpty()) {
            throw new RuntimeException("收货人姓名不能为空");
        }

        // 手机号不能为空
        if (userAddressDTO.getPhone() == null || userAddressDTO.getPhone().trim().isEmpty()) {
            throw new RuntimeException("手机号不能为空");
        }

        // 允许省份为空（特别是海外地址），若为 null 则初始化为空串，避免数据库 NOT NULL 约束报错
        if (userAddressDTO.getProvince() == null) {
            userAddressDTO.setProvince("");
        }

        // 城市不能为空
        if (userAddressDTO.getCity() == null || userAddressDTO.getCity().trim().isEmpty()) {
            throw new RuntimeException("城市不能为空");
        }

        // 允许区/县为空（特别是海外地址），若为 null 则初始化为空串，避免数据库 NOT NULL 约束报错
        if (userAddressDTO.getDistrict() == null) {
            userAddressDTO.setDistrict("");
        }

        // 详细地址不能为空
        if (userAddressDTO.getDetailAddress() == null || userAddressDTO.getDetailAddress().trim().isEmpty()) {
            throw new RuntimeException("详细地址不能为空");
        }

        // 是否默认地址不能为空
        if (userAddressDTO.getIsDefault() == null) {
            throw new RuntimeException("是否默认地址不能为空");
        }

        // 是否默认地址只能为0或1
        if (userAddressDTO.getIsDefault() != 0 && userAddressDTO.getIsDefault() != 1) {
            throw new RuntimeException("是否默认地址只能为0或1");
        }
    }
}
