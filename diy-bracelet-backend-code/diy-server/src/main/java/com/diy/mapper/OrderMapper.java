package com.diy.mapper;

import com.github.pagehelper.Page;
import com.diy.dto.OrdersPageQueryDTO;
import com.diy.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单
     */
    void insert(Orders orders);
    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from `order` where order_no = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页查询
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据ID查询订单详情
     */
    Orders getDetailsById(Long id);

    /**
     * 根据ID查询订单详情并加排他锁（防止并发退款）
     */
    @Select("select * from `order` where id=#{id} for update")
    Orders getDetailsByIdForUpdate(Long id);
    
    /**
     * 根据状态统计订单数量
     */
    @Select("select count(id) from `order` where status=#{status}")
    Integer countStatus(Integer status);
    
    /**
     * 根据订单号和用户ID查询
     */
    @Select("select * from `order` where order_no = #{orderNumber} and user_id= #{userId}")
    Orders getByNumberAndUserId(String orderNumber, Long userId);
    
    /**
     * 更新订单状态
     */
    @Update("update `order` set status=#{status} where id=#{id}")
    void updateStatus(Orders order);
    
    /**
     * 根据状态和时间查询订单
     */
    @Select("select * from `order` where status=#{pendingPayment} and create_time<#{time}")
    List<Orders> getByStatusAndOrdertimeLT(Integer pendingPayment, LocalDateTime time);

    /**
     * 依据动态条件统计金额
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 依据动态条件统计数量
     */
    Integer countByMap(Map map);
    
    /**
     * 根据交易号查询订单
     * @param transactionId 交易号
     */
    @Select("select * from `order` where transaction_id = #{transactionId}")
    Orders getByTransactionId(String transactionId);

    /**
     * 根据PayPal订单ID查询订单
     * @param paypalOrderId PayPal订单ID
     */
    @Select("select * from `order` where paypal_order_id = #{paypalOrderId}")
    Orders getByPaypalOrderId(String paypalOrderId);

    /**
     * 根据邮箱和/或订单号查询订单列表
     */
    List<Orders> getByEmailOrOrderNo(String email, String orderNo);
}
