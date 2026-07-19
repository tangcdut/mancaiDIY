package com.diy.controller.user;

import com.diy.dto.UserAddressDTO;
import com.diy.result.Result;
import com.diy.service.UserAddressService;
import com.diy.vo.UserAddressVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端用户地址管理接口
 */
@RestController
@RequestMapping("/user/address")
@Api(tags = "C端用户地址管理")
@Slf4j
public class UserAddressController {

    @Autowired
    private UserAddressService userAddressService;

    /**
     * 添加地址
     * @param userAddressDTO 地址信息
     * @return 添加后的地址信息
     */
    @PostMapping("/add")
    @ApiOperation("添加地址")
    public Result<UserAddressVO> addAddress(
            @ApiParam("地址信息") @Validated @RequestBody UserAddressDTO userAddressDTO) {
        log.info("添加地址: {}", userAddressDTO);
        UserAddressVO addressVO = userAddressService.addAddress(userAddressDTO);
        return Result.success(addressVO);
    }

    /**
     * 更新地址
     * @param userAddressDTO 地址信息
     * @return 更新后的地址信息
     */
    @PutMapping("/update")
    @ApiOperation("更新地址")
    public Result<UserAddressVO> updateAddress(
            @ApiParam("地址信息") @Validated @RequestBody UserAddressDTO userAddressDTO) {
        log.info("更新地址: {}", userAddressDTO);
        UserAddressVO addressVO = userAddressService.updateAddress(userAddressDTO);
        return Result.success(addressVO);
    }

    /**
     * 删除地址
     * @param id 地址ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{id}")
    @ApiOperation("删除地址")
    public Result deleteAddress(
            @ApiParam("地址ID") @PathVariable Long id) {
        log.info("删除地址，地址ID: {}", id);
        userAddressService.deleteAddress(id);
        return Result.success();
    }

    /**
     * 查询用户的所有地址
     * @return 地址列表
     */
    @GetMapping("/list")
    @ApiOperation("查询用户的所有地址")
    public Result<List<UserAddressVO>> listAddress() {
        log.info("查询用户地址列表");
        List<UserAddressVO> addressList = userAddressService.listAddress();
        return Result.success(addressList);
    }

    /**
     * 根据ID查询地址
     * @param id 地址ID
     * @return 地址信息
     */
    @GetMapping("/get/{id}")
    @ApiOperation("根据ID查询地址")
    public Result<UserAddressVO> getAddressById(
            @ApiParam("地址ID") @PathVariable Long id) {
        log.info("查询地址详情，地址ID: {}", id);
        UserAddressVO addressVO = userAddressService.getAddressById(id);
        return Result.success(addressVO);
    }

    /**
     * 设置默认地址
     * @param id 地址ID
     * @return 操作结果
     */
    @PutMapping("/setDefault/{id}")
    @ApiOperation("设置默认地址")
    public Result setDefaultAddress(
            @ApiParam("地址ID") @PathVariable Long id) {
        log.info("设置默认地址，地址ID: {}", id);
        userAddressService.setDefaultAddress(id);
        return Result.success();
    }

    /**
     * 获取默认地址
     * @return 默认地址
     */
    @GetMapping("/default")
    @ApiOperation("获取默认地址")
    public Result<UserAddressVO> getDefaultAddress() {
        log.info("获取默认地址");
        UserAddressVO defaultAddress = userAddressService.getDefaultAddress();
        return Result.success(defaultAddress);
    }
}
