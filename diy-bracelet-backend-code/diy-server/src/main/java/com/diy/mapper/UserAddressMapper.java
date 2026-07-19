package com.diy.mapper;

import com.diy.entity.UserAddress;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserAddressMapper {

    /**
     * 查询用户的所有地址
     * @param userId 用户ID
     * @return 地址列表
     */
    @Select("select * from user_address where user_id = #{userId} order by is_default desc, create_time desc")
    List<UserAddress> listByUserId(Long userId);

    /**
     * 根据ID查询地址
     * @param id 地址ID
     * @return 地址信息
     */
    @Select("select * from user_address where id = #{id}")
    UserAddress getById(Long id);

    /**
     * 插入地址
     * @param userAddress 地址信息
     */
    @Insert("insert into user_address(user_id, consignee, phone, province, city, district, detail_address, is_default, create_time, update_time) " +
            "values (#{userId}, #{consignee}, #{phone}, #{province}, #{city}, #{district}, #{detailAddress}, #{isDefault}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UserAddress userAddress);

    /**
     * 更新地址
     * @param userAddress 地址信息
     */
    @Update("update user_address set consignee = #{consignee}, phone = #{phone}, province = #{province}, " +
            "city = #{city}, district = #{district}, detail_address = #{detailAddress}, is_default = #{isDefault}, " +
            "update_time = #{updateTime} where id = #{id}")
    void update(UserAddress userAddress);

    /**
     * 删除地址
     * @param id 地址ID
     */
    @Delete("delete from user_address where id = #{id}")
    void deleteById(Long id);

    /**
     * 将用户的所有地址设置为非默认
     * @param userId 用户ID
     */
    @Update("update user_address set is_default = 0 where user_id = #{userId}")
    void clearDefaultByUserId(Long userId);

    /**
     * 查询用户的默认地址
     * @param userId 用户ID
     * @return 默认地址
     */
    @Select("select * from user_address where user_id = #{userId} and is_default = 1 limit 1")
    UserAddress getDefaultByUserId(Long userId);
}
