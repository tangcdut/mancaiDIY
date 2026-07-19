package com.diy.mapper;

import com.diy.entity.OverseasOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 海外版订单商品明细Mapper
 */
@Mapper
public interface OverseasOrderItemMapper {

    /**
     * 批量插入订单明细
     */
    void insertBatch(@Param("items") List<OverseasOrderItem> items);

    /**
     * 根据订单ID查询明细
     */
    @Select("select * from overseas_order_item where order_id = #{orderId}")
    List<OverseasOrderItem> getByOrderId(@Param("orderId") Long orderId);
}
