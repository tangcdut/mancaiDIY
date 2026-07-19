package com.diy.service;

import com.diy.dto.*;
import com.diy.result.PageResult;
import com.diy.vo.OrderCreateVO;
import com.diy.vo.OrderPaymentStatusVO;
import com.diy.vo.OrderPaymentVO;
import com.diy.vo.OrderSubmitVO;
import com.diy.vo.OrderVO;

public interface OrderService {
    /**
     * 用户下单
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);
    
    /**
     * 从购物车创建订单
     */
    OrderCreateVO createOrderFromCart(OrdersSubmitDTO ordersSubmitDTO);
    
    /**
     * 订单支付
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo 商户订单号
     * @param transactionId 微信支付交易号
     */
    void paySuccess(String outTradeNo, String transactionId);

    /**
     * 用户端分页查询订单
     */
    PageResult pageQuery4User(OrdersPageQueryDTO ordersPageQueryDTO);
    
    /**
     * 分页查询订单列表（新接口）
     */
    PageResult listOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 查看订单详情
     */
    OrderVO getDetails(Long id);

    /**
     * 取消订单
     */
    void update(Long id);

    /**
     * 再来一单
     */
    void repetition(Long id);

    /**
     * 管理端条件查询订单
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 发货
     */
    void delivery(Long id);

    /**
     * 完成订单
     */
    void complete(Long id);

    /**
     * 取消订单
     */
    void cancel(OrdersCancelDTO ordersCancelDTO);

    /**
     * 用户催单
     */
    void reminder(Long id);
    
    /**
     * 更新订单状态
     * @param orderStatusUpdateDTO 订单状态更新信息
     */
    void updateStatus(OrderStatusUpdateDTO orderStatusUpdateDTO);
    
    /**
     * 查询订单支付状态
     * @param orderNo 订单号
     * @return 订单支付状态信息
     */
    OrderPaymentStatusVO getPaymentStatus(String orderNo);
    
    /**
     * 申请退款
     * @param orderId 订单ID
     */
    void refund(Long orderId) throws Exception;
    
    /**
     * 退款成功回调，更新订单状态并回滚库存
     * @param outTradeNo 商户订单号
     */
    void refundSuccess(String outTradeNo);
    
    /**
     * 管理员审核通过退款，实际执行微信退款
     * @param orderId 订单ID
     * @param adminPhone 管理员手机号（用于验证）
     */
    void adminRefund(Long orderId, String adminPhone) throws Exception;
    
    /**
     * PayPal支付
     * @param orderNo 订单号
     * @param currencyCode 货币代码（如USD、EUR等）
     * @return PayPal订单ID和支付链接
     */
    OrderPaymentVO paypalPayment(String orderNo, String currencyCode) throws Exception;
    
    /**
     * PayPal支付成功回调
     * @param paypalOrderId PayPal订单ID
     * @param payerId 支付者ID
     */
    void paypalPaySuccess(String paypalOrderId, String payerId) throws Exception;

    /**
     * 免登录创建订单（海外版）
     */
    com.diy.controller.guest.GuestOrderController.GuestOrderCreateVO createGuestOrder(GuestOrderCreateDTO dto) throws Exception;

    /**
     * 免登录PayPal支付（海外版）
     */
    OrderPaymentVO guestPaypalPayment(String orderNo, String currencyCode) throws Exception;

    /**
     * 免登录查询订单详情（海外版）
     */
    OrderVO getGuestOrderDetail(String orderNo);

    /**
     * 免登录查询订单列表（海外版）
     */
    java.util.List<OrderVO> getGuestOrderList(String email, String orderNo);
}