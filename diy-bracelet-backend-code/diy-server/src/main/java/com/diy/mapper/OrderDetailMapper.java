package com.diy.mapper;

import com.diy.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /**
     * 批量插入订单明细
     */
    void insertBatch(@Param("orderItemList") List<OrderItem> orderItemList);
    
    /**
     * 根据订单ID查询订单明细
     */
    @Select("select * from order_item where order_id = #{orderId}")
    List<OrderItem> getByOrderId(Long orderId);
}
