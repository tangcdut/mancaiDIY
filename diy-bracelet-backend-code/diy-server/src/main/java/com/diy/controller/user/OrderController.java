package com.diy.controller.user;

import com.diy.dto.OrdersPageQueryDTO;
import com.diy.dto.OrdersPaymentDTO;
import com.diy.dto.OrdersSubmitDTO;
import com.diy.entity.Orders;
import com.diy.result.PageResult;
import com.diy.result.Result;
import com.diy.service.OrderService;
import com.diy.vo.OrderCreateVO;
import com.diy.vo.OrderDetailVO;
import com.diy.vo.OrderListVO;
import com.diy.vo.OrderPaymentStatusVO;
import com.diy.vo.OrderPaymentVO;
import com.diy.vo.OrderSubmitVO;
import com.diy.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "用户订单相关")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;


    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
//    @ApiOperation("用户下单")
//    @PostMapping("/submit")
//    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
//        log.info("用户下单,参数为:{}", ordersSubmitDTO);
//        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
//        return Result.success(orderSubmitVO);
//    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        // 注意：不要在这里调用paySuccess！应该等待微信支付回调后才更新订单状态
        // paySuccess只能在PayNotifyController的回调中调用
        return Result.success(orderPaymentVO);
    }
    
    /**
     * 查询订单支付状态
     * 用于前端轮询或用户从微信支付返回后查询支付结果
     * 
     * @param orderNo 订单号
     * @return 订单支付状态信息
     */
    @GetMapping("/paymentStatus")
    @ApiOperation("查询订单支付状态")
    public Result<OrderPaymentStatusVO> getPaymentStatus(
            @ApiParam(value = "订单号", required = true) @RequestParam String orderNo) {
        log.info("查询订单支付状态，订单号: {}", orderNo);
        OrderPaymentStatusVO paymentStatus = orderService.getPaymentStatus(orderNo);
        return Result.success(paymentStatus);
    }
    
    /**
     * 历史订单查询
     *
     * @return
     */
//    @GetMapping("/historyOrders")
//    public Result<PageResult> historyorders(OrdersPageQueryDTO ordersPageQueryDTO) {
//
//        PageResult pageResult = orderService.pageQuery4User(ordersPageQueryDTO);
//        return Result.success(pageResult);
//    }
    
    /**
     * 获取订单状态文本
     * @param status 订单状态码
     * @return 状态文本
     */
    private String getStatusTextForOrder(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        switch (status) {
            case 0: // PENDING_PAYMENT
                return "待支付";
            case 1: // PAID
                return "已支付";
            case 2: // SHIPPED
                return "已发货";
            case 3: // COMPLETED
                return "已完成";
            case 4: // REFUNDING
                return "退款中";
            case 5: // REFUNDED
                return "已退款";
            case 6: // CANCELLED
                return "已取消";
            default:
                return "未知状态";
        }
    }
    
    /**
     * 分页查询订单列表
     * @param page 页码（可选，默认1）
     * @param size 每页大小（可选，默认20）
     * @param status 订单状态（可选）
     * @param orderNo 订单编号（可选，支持模糊查询）
     * @return
     */
    @GetMapping("/list")
        @ApiOperation("分页查询订单列表（支持按状态0-6筛选）")
    public Result<OrderListVO> listOrders(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam(value = "每页大小", defaultValue = "20") @RequestParam(defaultValue = "20") Integer size,
            @ApiParam(value = "订单状态（0-6）") @RequestParam(required = false) Integer status,
            @ApiParam(value = "订单编号（支持模糊查询）") @RequestParam(required = false) String orderNo) {
        
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setPage(page);
        ordersPageQueryDTO.setPageSize(size);
        if (status != null) {
            ordersPageQueryDTO.setStatus(status);
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            ordersPageQueryDTO.setOrderNo(orderNo.trim());
        }
        
        PageResult pageResult = orderService.listOrders(ordersPageQueryDTO);
        
        // 转换为VO
        List<OrderListVO.OrderItem> orderItems = new ArrayList<>();
        if (pageResult.getRecords() != null) {
            for (Object obj : pageResult.getRecords()) {
                if (obj instanceof Orders) {
                    Orders orders = (Orders) obj;
                    
                    // 拼接完整地址
                    String receiverAddress = "";
                    if (orders.getReceiverProvince() != null || orders.getReceiverCity() != null || 
                        orders.getReceiverDistrict() != null || orders.getReceiverDetail() != null) {
                        receiverAddress = (orders.getReceiverProvince() != null ? orders.getReceiverProvince() : "") +
                                         (orders.getReceiverCity() != null ? orders.getReceiverCity() : "") +
                                         (orders.getReceiverDistrict() != null ? orders.getReceiverDistrict() : "") +
                                         (orders.getReceiverDetail() != null ? orders.getReceiverDetail() : "");
                    }
                    
                    // 获取状态文本
                    String statusText = getStatusTextForOrder(orders.getStatus());
                    
                    OrderListVO.OrderItem item = OrderListVO.OrderItem.builder()
                            .orderId(orders.getId())
                            .orderNo(orders.getOrderNo())
                            .amount(orders.getAmount())
                            .status(orders.getStatus())
                            .statusText(statusText)
                            .createTime(orders.getCreateTime())
                            .payTime(orders.getPayTime())
                            .receiverName(orders.getReceiverName())
                            .receiverPhone(orders.getReceiverPhone())
                            .receiverAddress(receiverAddress)
                            .productImage(orders.getProductImage())
                            .trackingNumber(orders.getTrackingNumber()) // 添加运单号字段
                            .build();
                    orderItems.add(item);
                }
            }
        }
        
        OrderListVO orderListVO = OrderListVO.builder()
                .orders(orderItems)
                .page(page)
                .size(size)
                .total(pageResult.getTotal())
                .build();
        
        return Result.success(orderListVO);
    }
    

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        OrderVO orderdetail = orderService.getDetails(id);
        return Result.success(orderdetail);
    }
    
    /**
     * 根据orderId查看订单详情
     * 
     * @param orderId 订单ID
     * @return
     */
    @GetMapping("/detail")
    @ApiOperation("根据orderId查看订单详情")
    public Result<OrderDetailVO> getOrderDetail(
            @ApiParam(value = "订单ID", required = true) @RequestParam Long orderId) {
        
        // 获取订单详情
        OrderVO orderVO = orderService.getDetails(orderId);
        
        // 转换为新的VO格式
        OrderDetailVO.Order order = OrderDetailVO.Order.builder()
                .orderId(orderVO.getId())
                .orderNo(orderVO.getOrderNo())
                .amount(orderVO.getAmount())
                .status(orderVO.getStatus())
                .createTime(orderVO.getCreateTime())
                .payTime(orderVO.getPayTime())
                .deliveryTime(orderVO.getDeliveryTime())
                .completeTime(orderVO.getCompleteTime())
                .receiverName(orderVO.getReceiverName())
                .receiverPhone(orderVO.getReceiverPhone())
                .receiverProvince(orderVO.getReceiverProvince())
                .receiverCity(orderVO.getReceiverCity())
                .receiverDistrict(orderVO.getReceiverDistrict())
                .receiverDetail(orderVO.getReceiverDetail())
                .transactionId(orderVO.getTransactionId())
                .refundAmount(orderVO.getRefundAmount())
                .refundTime(orderVO.getRefundTime())
                .refundReason(orderVO.getRefundReason())
                .refundId(orderVO.getRefundId())
                .remark(orderVO.getRemark())
                .productImage(orderVO.getProductImage())
                .description(orderVO.getDescription())
                .trackingNumber(orderVO.getTrackingNumber()) // 添加运单号字段
                .build();

        // 转换订单项
        List<OrderDetailVO.Item> items = new ArrayList<>();
        if (orderVO.getOrderItems() != null) {
            for (com.diy.entity.OrderItem orderItem : orderVO.getOrderItems()) {
                OrderDetailVO.Item detailItem = OrderDetailVO.Item.builder()
                        .productId(orderItem.getProductId())
                        .title(orderItem.getTitle())
                        .price(orderItem.getPrice())
                        .quantity(orderItem.getQuantity())
                        .productImage(orderItem.getProductImage())
                        .build();
                items.add(detailItem);
            }
        }
        order.setItems(items);
        
        OrderDetailVO orderDetailVO = new OrderDetailVO();
        orderDetailVO.setOrder(order);
        
        return Result.success(orderDetailVO);
    }

    /**
     * 取消订单
     *
     * @param id
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancel(@PathVariable Long id) {
        orderService.update(id);
        return Result.success();
    }
    
    /**
     * 申请退款
     * 
     * @param id 订单ID
     * @return
     */
    @PostMapping("/refund/{id}")
    @ApiOperation("申请退款")
    public Result refund(@PathVariable Long id) throws Exception {
        log.info("申请退款，订单ID: {}", id);
        orderService.refund(id);
        return Result.success();
    }
    
    /**
     * 确认收货
     *
     * @param id
     * @return
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("确认收货")
    public Result complete(@PathVariable Long id) {
        log.info("确认收货，订单ID: {}", id);
        orderService.complete(id);
        return Result.success();
    }

    /**
     * 再来一单
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable Long id) {
        log.info("再来一单，原订单ID: {}", id);
        orderService.repetition(id);
        return Result.success();
    }
    
    /**
     * 从购物车创建订单
     * @return
     */
    @PostMapping("/create")
    @ApiOperation("从购物车创建订单")
    public Result<OrderCreateVO> createOrderFromCart(@RequestBody(required = false) OrdersSubmitDTO ordersSubmitDTO) {
        OrderCreateVO orderCreateVO = orderService.createOrderFromCart(ordersSubmitDTO);
        return Result.success(orderCreateVO);
    }
    
    /**
     * 催单
     * @param id 订单ID
     * @return
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("催单")
    public Result reminder(@PathVariable("id") Long id){
        log.info("用户催单，订单ID: {}", id);
        orderService.reminder(id);
        return Result.success();
    }
    
    /**
     * PayPal支付
     * 
     * @param orderNo 订单号
     * @param currencyCode 货币代码（如USD、EUR等）
     * @return PayPal订单ID和支付链接
     */
    @PostMapping("/paypalPayment")
    @ApiOperation("PayPal支付")
    public Result<OrderPaymentVO> paypalPayment(
            @ApiParam(value = "订单号", required = true) @RequestParam String orderNo,
            @ApiParam(value = "货币代码", required = true, defaultValue = "USD") @RequestParam(defaultValue = "USD") String currencyCode) throws Exception {
        log.info("PayPal支付，订单号: {}, 货币代码: {}", orderNo, currencyCode);
        OrderPaymentVO orderPaymentVO = orderService.paypalPayment(orderNo, currencyCode);
        log.info("PayPal支付创建成功，PayPal订单ID: {}", orderPaymentVO.getPaypalOrderId());
        return Result.success(orderPaymentVO);
    }
}
