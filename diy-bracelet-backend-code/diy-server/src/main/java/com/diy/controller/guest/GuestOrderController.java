package com.diy.controller.guest;

import com.diy.dto.GuestOrderCreateDTO;
import com.diy.result.Result;
import com.diy.service.OrderService;
import com.diy.vo.OrderPaymentStatusVO;
import com.diy.vo.OrderPaymentVO;
import com.diy.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Guest (no-login) order controller for overseas H5
 * All endpoints under /guest/** bypass JWT authentication
 */
@RestController("guestOrderController")
@RequestMapping("/guest/order")
@Api(tags = "海外版免登录订单")
@Slf4j
public class GuestOrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Create order as guest (no login)
     */
    @PostMapping("/create")
    @ApiOperation("免登录创建订单")
    public Result<GuestOrderCreateVO> create(@RequestBody GuestOrderCreateDTO dto) throws Exception {
        log.info("免登录创建订单：items={}, amount={}",
            dto.getItems() != null ? dto.getItems().size() : 0, dto.getAmount());
        GuestOrderCreateVO vo = orderService.createGuestOrder(dto);
        return Result.success(vo);
    }

    /**
     * Create PayPal payment for guest order
     */
    @PostMapping("/paypalPayment")
    @ApiOperation("免登录PayPal支付")
    public Result<OrderPaymentVO> paypalPayment(
            @RequestParam String orderNo,
            @RequestParam(defaultValue = "USD") String currencyCode) throws Exception {
        log.info("免登录PayPal支付，订单号: {}, 货币: {}", orderNo, currencyCode);
        OrderPaymentVO vo = orderService.guestPaypalPayment(orderNo, currencyCode);
        log.info("PayPal支付创建成功，PayPal订单ID: {}", vo.getPaypalOrderId());
        return Result.success(vo);
    }

    /**
     * Get guest order detail by order number
     */
    @GetMapping("/detail")
    @ApiOperation("免登录查询订单详情")
    public Result<OrderVO> detail(@RequestParam String orderNo) {
        log.info("免登录查询订单详情，订单号: {}", orderNo);
        OrderVO vo = orderService.getGuestOrderDetail(orderNo);
        return Result.success(vo);
    }

    /**
     * ★ 查询订单支付状态（主动查询兜底）
     * 如果 PayPal 已审批但未 capture（如用户关闭了浏览器），主动完成 capture
     */
    @GetMapping("/paymentStatus")
    @ApiOperation("免登录查询支付状态")
    public Result<OrderPaymentStatusVO> paymentStatus(@RequestParam String orderNo) {
        log.info("免登录查询支付状态，订单号: {}", orderNo);
        OrderPaymentStatusVO vo = orderService.getPaymentStatus(orderNo);
        return Result.success(vo);
    }

    /**
     * Get guest order list by email or order number
     */
    @GetMapping("/list")
    @ApiOperation("免登录查询订单列表")
    public Result<java.util.List<OrderVO>> list(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String orderNo) {
        log.info("免登录查询订单列表，email: {}, orderNo: {}", email, orderNo);
        java.util.List<OrderVO> list = orderService.getGuestOrderList(email, orderNo);
        return Result.success(list);
    }

    /**
     * Simple VO for guest order creation response
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GuestOrderCreateVO {
        private Long orderId;
        private String orderNo;
        private java.math.BigDecimal amount;
    }
}
