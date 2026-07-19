package com.diy.mapper;

import com.diy.entity.OverseasOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 海外版订单Mapper
 */
@Mapper
public interface OverseasOrderMapper {

    /**
     * 插入订单
     */
    void insert(OverseasOrder order);

    /**
     * 更新订单
     */
    void update(OverseasOrder order);

    /**
     * 根据订单号查询
     */
    @Select("select * from overseas_order where order_no = #{orderNo}")
    OverseasOrder getByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据PayPal订单ID查询
     */
    @Select("select * from overseas_order where paypal_order_id = #{paypalOrderId}")
    OverseasOrder getByPaypalOrderId(@Param("paypalOrderId") String paypalOrderId);

    /**
     * 根据ID查询
     */
    @Select("select * from overseas_order where id = #{id}")
    OverseasOrder getById(@Param("id") Long id);

    /**
     * 查询所有订单
     */
    @Select("select * from overseas_order order by create_time desc")
    List<OverseasOrder> findAll();
}
