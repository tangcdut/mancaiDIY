package com.diy.mapper;

import com.diy.entity.CartItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ShoppingCartMapper {
    /**
     * 查询购物车列表
     */
    List<CartItem> list(CartItem cartItem);
    
    /**
     * 查询购物车列表（包含商品信息）
     */
    List<Map<String, Object>> listWithProductInfo(Long userId);
    
    /**
     * 更新购物车数量
     */
    @Update("update cart_item set quantity=#{quantity} where id=#{id}")
    void updateQuantityById(CartItem cartItem);

    /**
     * 插入购物车
     * @param cartItem
     */
    @Insert("insert into cart_item(user_id, product_id, quantity, create_time) " +
        "values " +
        "(#{userId},#{productId},#{quantity},#{createTime})")
    void insert(CartItem cartItem);

    /**
     * 根据用户Id清空购物车
     * @param userId
     */
    @Delete("delete from cart_item where user_id=#{userId}")
    void deleteByUserId(Long userId);
    
    @Delete("delete from cart_item where id=#{id}")
    void deleteById(Long id);
}
