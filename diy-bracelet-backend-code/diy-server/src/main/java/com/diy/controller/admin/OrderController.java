package com.diy.controller.admin;

import com.diy.dto.OrderStatusUpdateDTO;
import com.diy.dto.OrdersCancelDTO;
import com.diy.dto.OrdersPageQueryDTO;
import com.diy.dto.OrdersRejectionDTO;
import com.diy.entity.OrderItem;
import com.diy.entity.OverseasOrder;
import com.diy.entity.OverseasOrderItem;
import com.diy.entity.Orders;
import com.diy.mapper.OverseasOrderItemMapper;
import com.diy.mapper.OverseasOrderMapper;
import com.diy.result.PageResult;
import com.diy.result.Result;
import com.diy.service.OrderService;
import com.diy.vo.OrderDetailAdminVO;
import com.diy.vo.OrderPageVO;
import com.diy.vo.OrderStatisticsVO;
import com.diy.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "订单管理")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Value("${diy.order.allow-completed-refund:true}")
    private boolean allowCompletedRefund;

    @Autowired
    private OverseasOrderMapper overseasOrderMapper;

    @Autowired
    private OverseasOrderItemMapper overseasOrderItemMapper;

    /**
     * 订单分页查询
     * @param ordersPageQueryDTO 分页查询条件
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("订单分页查询")
    public Result<OrderPageVO> list(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("订单分页查询: {}", ordersPageQueryDTO);
        
        // 设置默认分页参数
        if (ordersPageQueryDTO.getPage() <= 0) {
            ordersPageQueryDTO.setPage(1);
        }
        if (ordersPageQueryDTO.getPageSize() <= 0) {
            ordersPageQueryDTO.setPageSize(20);
        }
        
        // 分页查询（管理端不需要userId限制）
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        
        // 转换为VO
        List<OrderPageVO.OrderItem> orderItems = new ArrayList<>();
        if (pageResult.getRecords() != null) {
            for (Object obj : pageResult.getRecords()) {
                if (obj instanceof OrderVO) {
                    OrderVO orderVO = (OrderVO) obj;
                    
                    // 拼接完整地址
                    String receiverAddress = "";
                    if (orderVO.getReceiverProvince() != null || orderVO.getReceiverCity() != null || 
                        orderVO.getReceiverDistrict() != null || orderVO.getReceiverDetail() != null) {
                        receiverAddress = (orderVO.getReceiverProvince() != null ? orderVO.getReceiverProvince() : "") +
                                         (orderVO.getReceiverCity() != null ? orderVO.getReceiverCity() : "") +
                                         (orderVO.getReceiverDistrict() != null ? orderVO.getReceiverDistrict() : "") +
                                         (orderVO.getReceiverDetail() != null ? orderVO.getReceiverDetail() : "");
                    }
                    
                    OrderPageVO.OrderItem item = OrderPageVO.OrderItem.builder()
                            .orderId(orderVO.getId())
                            .orderNo(orderVO.getOrderNo())
                            .amount(orderVO.getAmount())
                            .status(orderVO.getStatus())
                            .createTime(orderVO.getCreateTime())
                            .payTime(orderVO.getPayTime())
                            .receiverName(orderVO.getReceiverName())
                            .receiverPhone(orderVO.getReceiverPhone())
                            .receiverAddress(receiverAddress)
                            .trackingNumber(orderVO.getTrackingNumber()) // 添加运单号字段
                            .isOverseas(orderVO.getIsOverseas())
                            .build();
                    orderItems.add(item);
                }
            }
        }
        
        OrderPageVO orderPageVO = OrderPageVO.builder()
                .orders(orderItems)
                .page(ordersPageQueryDTO.getPage())
                .size(ordersPageQueryDTO.getPageSize())
                .total(pageResult.getTotal())
                .build();
        
        return Result.success(orderPageVO);
    }

    /**
     * 查看订单详情
     * @param orderId 订单ID
     * @return
     */
    @GetMapping("/detail")
    @ApiOperation("查看订单详情")
    public Result<OrderDetailAdminVO> detail(@RequestParam Long orderId) {
        log.info("查看订单详情: orderId={}", orderId);
        
        // 获取订单详情
        OrderVO orderVO = orderService.getDetails(orderId);
        
        // 转换为管理员端VO格式
        OrderDetailAdminVO.Order order = OrderDetailAdminVO.Order.builder()
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
                .isOverseas(orderVO.getIsOverseas())
                .build();

        // 转换订单项
        List<OrderDetailAdminVO.Item> items = new ArrayList<>();
        if (orderVO.getOrderItems() != null) {
            for (com.diy.entity.OrderItem orderItem : orderVO.getOrderItems()) {
                OrderDetailAdminVO.Item detailItem = OrderDetailAdminVO.Item.builder()
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
        
        OrderDetailAdminVO orderDetailAdminVO = new OrderDetailAdminVO();
        orderDetailAdminVO.setOrder(order);
        
        return Result.success(orderDetailAdminVO);
    }

    /**
     * 更新订单状态
     * @param orderStatusUpdateDTO 订单状态更新信息
     * @return
     */
    @PostMapping("/updateStatus")
    @ApiOperation("更新订单状态")
    public Result<String> updateStatus(@RequestBody OrderStatusUpdateDTO orderStatusUpdateDTO) {
        log.info("更新订单状态: {}", orderStatusUpdateDTO);
        orderService.updateStatus(orderStatusUpdateDTO);
        return Result.success();
    }

    /**
     * 条件查询订单
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("条件查询订单")
    public Result<PageResult> search(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 查看订单详情
     * @param id
     * @return
     */
//    @GetMapping("/details/{id}")
//    @ApiOperation("查看订单详情")
//    public Result<OrderVO> details(@PathVariable Long id){
//        OrderVO details = orderService.getDetails(id);
//        return Result.success(details);
//    }

    /**
     * 发货（支持运单号）
     * @param orderStatusUpdateDTO 订单状态更新信息（包含运单号）
     * @return
     */
    @PostMapping("/deliveryWithTrackingNumber")
    @ApiOperation("发货（支持运单号）")
    public Result deliveryWithTrackingNumber(@RequestBody OrderStatusUpdateDTO orderStatusUpdateDTO){
        log.info("发货（支持运单号）: {}", orderStatusUpdateDTO);
        orderService.updateStatus(orderStatusUpdateDTO);
        return Result.success();
    }

    /**
     * 发货
     * @param id
     * @return
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("发货")
    public Result delivery(@PathVariable Long id){
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     * @param id
     * @return
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable Long id){
        orderService.complete(id);
        return Result.success();
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     * @return
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO){
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }
    
    /**
     * 管理员审核通过退款
     * @param orderId 订单ID
     * @param adminPhone 管理员手机号（用于验证）
     * @return
     */
    @PostMapping("/adminRefund")
    @ApiOperation("管理员审核通过退款")
    public Result adminRefund(@RequestParam Long orderId, @RequestParam String adminPhone) throws Exception {
        log.info("管理员审核通过退款，订单ID: {}，管理员手机号: {}", orderId, adminPhone);
        orderService.adminRefund(orderId, adminPhone);
        return Result.success();
    }

    /**
     * 获取退款配置（是否允许已完成订单退款）
     * @return
     */
    @GetMapping("/refundConfig")
    @ApiOperation("获取退款配置（是否允许已完成订单退款）")
    public Result<Boolean> getRefundConfig() {
        log.info("获取退款配置: {}", allowCompletedRefund);
        return Result.success(allowCompletedRefund);
    }

    // ============ 海外订单管理 ============

    /**
     * 海外订单列表
     */
    @GetMapping("/overseas/list")
    @ApiOperation("海外订单列表（分页）")
    public Result<Map<String, Object>> overseasList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        log.info("海外订单列表: page={}, pageSize={}, status={}", page, pageSize, status);

        // Since overseas orders are relatively few, load all and paginate in memory
        // In production, add proper pagination to the mapper
        List<OverseasOrder> allOrders = overseasOrderMapper.findAll();
        if (status != null) {
            allOrders.removeIf(o -> !status.equals(o.getStatus()));
        }

        // Sort by createTime desc
        allOrders.sort((a, b) -> {
            if (a.getCreateTime() == null || b.getCreateTime() == null) return 0;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });

        int total = allOrders.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<OverseasOrder> pageOrders = fromIndex < total ? allOrders.subList(fromIndex, toIndex) : new ArrayList<>();

        Map<String, Object> result = new HashMap<>();
        result.put("orders", pageOrders);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    /**
     * 更新海外订单状态
     */
    @PostMapping("/overseas/updateStatus")
    @ApiOperation("更新海外订单状态")
    public Result<String> overseasUpdateStatus(@RequestBody Map<String, Object> body) throws Exception {
        Long orderId = body.get("orderId") != null ? Long.valueOf(body.get("orderId").toString()) : null;
        Integer status = body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : null;

        log.info("更新海外订单状态: orderId={}, status={}", orderId, status);

        if (orderId == null || status == null) {
            return Result.error("orderId and status are required");
        }

        OverseasOrder order = overseasOrderMapper.getById(orderId);
        if (order == null) {
            return Result.error("Order not found");
        }

        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        overseasOrderMapper.update(order);

        return Result.success();
    }
}
