package com.diy.controller.notify;

import com.alibaba.fastjson.JSONObject;
import com.diy.entity.OverseasOrder;
import com.diy.entity.Orders;
import com.diy.mapper.OverseasOrderMapper;
import com.diy.mapper.OrderMapper;
import com.diy.mapper.PayPalWebhookEventMapper;
import com.diy.service.OrderService;
import com.diy.utils.PayPalPayUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.time.LocalDateTime;

/**
 * PayPal支付回调相关接口
 */
@RestController
@RequestMapping("/paypal")
@Slf4j
@Api(tags = "PayPal支付回调接口")
public class PayPalPayController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private PayPalPayUtil payPalPayUtil;
    @Autowired
    private OverseasOrderMapper overseasOrderMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private PayPalWebhookEventMapper paypalWebhookEventMapper;

    /**
     * PayPal支付成功回调（从PayPal重定向回来）
     * 处理完成后重定向到 H5 订单详情页，而不是返回 JSON
     */
    @GetMapping("/paySuccess")
    @ApiOperation("PayPal支付成功回调")
    public void paySuccessNotify(
            @RequestParam(value = "paypal_order_id", required = false) String paypalOrderId,
            @RequestParam(value = "PayerID", required = false) String payerId,
            @RequestParam(value = "token", required = false) String token,
            HttpServletResponse response) {

        // PayPal auto-appends "token" (which is the PayPal order ID) to the return URL.
        // Use it as fallback when paypal_order_id is not explicitly provided.
        if (paypalOrderId == null || paypalOrderId.isEmpty()) {
            paypalOrderId = token;
        }

        log.info("PayPal支付成功回调，PayPal订单ID: {}, 支付者ID: {}, Token: {}",
            paypalOrderId, payerId, token);

        String orderNo = null;
        boolean captureSuccess = false;
        String errorMessage = null;

        try {
            orderService.paypalPaySuccess(paypalOrderId, payerId);

            // 查询订单号用于重定向
            Orders mainOrder = orderMapper.getByPaypalOrderId(paypalOrderId);
            if (mainOrder != null) {
                orderNo = mainOrder.getOrderNo();
            } else {
                OverseasOrder order = overseasOrderMapper.getByPaypalOrderId(paypalOrderId);
                if (order != null) {
                    orderNo = order.getOrderNo();
                }
            }

            captureSuccess = true;
            log.info("PayPal支付处理成功，PayPal订单ID: {}，订单号: {}", paypalOrderId, orderNo);
        } catch (Exception e) {
            log.error("PayPal支付回调处理失败，PayPal订单ID: {}", paypalOrderId, e);
            // 提取可读的错误信息
            errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.length() > 100) {
                errorMessage = errorMessage.substring(0, 100);
            }
            // 尝试从异常中获取订单号（海外订单可能在抛出异常前已查到）
            try {
                Orders mainOrder = orderMapper.getByPaypalOrderId(paypalOrderId);
                if (mainOrder != null) {
                    orderNo = mainOrder.getOrderNo();
                } else {
                    OverseasOrder order = overseasOrderMapper.getByPaypalOrderId(paypalOrderId);
                    if (order != null) {
                        orderNo = order.getOrderNo();
                    }
                }
            } catch (Exception ignored) {}
        }

        // ★ 重定向到 H5 订单详情页，附带支付结果参数
        try {
            String redirectBase = "https://diy.hk.cn/overseas/#/pages/order/detail";
            if (orderNo != null) {
                if (captureSuccess) {
                    response.sendRedirect(redirectBase + "?orderNo=" + orderNo + "&payment=success");
                } else {
                    String errParam = errorMessage != null
                        ? "&error=" + java.net.URLEncoder.encode(errorMessage, "UTF-8")
                        : "";
                    response.sendRedirect(redirectBase + "?orderNo=" + orderNo + "&payment=failed" + errParam);
                }
            } else {
                // 降级：重定向到首页
                response.sendRedirect("https://diy.hk.cn/overseas/");
            }
        } catch (Exception e) {
            log.error("重定向失败", e);
            try {
                response.setStatus(200);
                response.setHeader("Content-type", "application/json;charset=UTF-8");
                String respJson = captureSuccess
                    ? "{\"code\":1,\"message\":\"Payment processed\"}"
                    : "{\"code\":0,\"message\":\"Payment failed\"}";
                response.getOutputStream().write(respJson.getBytes());
                response.flushBuffer();
            } catch (Exception ex) {
                log.error("设置响应状态失败", ex);
            }
        }
    }

    /**
     * PayPal支付取消回调 — 重定向到购物车
     */
    @GetMapping("/payCancel")
    @ApiOperation("PayPal支付取消回调")
    public void payCancelNotify(HttpServletResponse response) {
        log.info("PayPal支付取消回调");
        try {
            response.sendRedirect("https://diy.hk.cn/overseas/#/pages/cart/index");
        } catch (Exception e) {
            log.error("重定向失败", e);
        }
    }

    /**
     * ★ PayPal Webhook回调（异步通知）— 完整实现
     * 1. 验证签名
     * 2. 解析事件类型
     * 3. 幂等处理
     * 4. 更新订单状态
     */
    @PostMapping("/webhook")
    @ApiOperation("PayPal Webhook回调")
    public void webhookNotify(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1. 读取原始请求体
            String body = readData(request);
            log.info("PayPal Webhook回调，Body: {}", body);

            // 2. 获取验证所需的 Header
            String authAlgo = request.getHeader("PAYPAL-AUTH-ALGO");
            String certUrl = request.getHeader("PAYPAL-CERT-URL");
            String transmissionId = request.getHeader("PAYPAL-TRANSMISSION-ID");
            String transmissionSig = request.getHeader("PAYPAL-TRANSMISSION-SIG");
            String transmissionTime = request.getHeader("PAYPAL-TRANSMISSION-TIME");

            // 3. ★ 验证 Webhook 签名（防止伪造）
            boolean verified = payPalPayUtil.verifyWebhookSignature(
                authAlgo, certUrl, transmissionId, transmissionSig, transmissionTime, body);

            if (!verified) {
                log.warn("PayPal Webhook签名验证失败，transmissionId: {}", transmissionId);
                response.setStatus(400);
                response.getOutputStream().write("{\"code\":0,\"message\":\"Signature verification failed\"}".getBytes());
                response.flushBuffer();
                return;
            }

            // 4. ★ 解析事件
            JSONObject event = JSONObject.parseObject(body);
            String eventType = event.getString("event_type");
            String eventId = event.getString("id");
            JSONObject resource = event.getJSONObject("resource");

            log.info("PayPal Webhook事件类型: {}, 事件ID: {}", eventType, eventId);

            // 5. ★ 幂等检查：防止重复处理
            if (paypalWebhookEventMapper.countByEventId(eventId) > 0) {
                log.info("PayPal Webhook事件已处理，跳过，事件ID: {}", eventId);
                response.setStatus(200);
                response.getOutputStream().write("{\"code\":1,\"message\":\"Already processed\"}".getBytes());
                response.flushBuffer();
                return;
            }

            // 6. ★ 根据事件类型处理
            boolean handled = false;
            String resourceId = null;

            if ("PAYMENT.CAPTURE.COMPLETED".equals(eventType)) {
                // 支付捕获成功
                resourceId = resource != null ? resource.getString("id") : null;
                String paypalOrderId = resource != null
                    ? resource.getJSONObject("supplementary_data")
                        .getJSONObject("related_ids")
                        .getString("order_id")
                    : null;

                log.info("处理 PAYMENT.CAPTURE.COMPLETED，捕获ID: {}，PayPal订单ID: {}", resourceId, paypalOrderId);

                if (paypalOrderId != null) {
                    Orders mainOrder = orderMapper.getByPaypalOrderId(paypalOrderId);
                    if (mainOrder != null) {
                        if (Orders.PENDING_PAYMENT.equals(mainOrder.getStatus())) {
                            mainOrder.setStatus(Orders.PAID);
                            mainOrder.setPaypalCaptureId(resourceId);
                            mainOrder.setPayTime(LocalDateTime.now());
                            mainOrder.setUpdateTime(LocalDateTime.now());
                            orderMapper.update(mainOrder);
                            log.info("Webhook: 主表海外订单 {} 已更新为已支付，捕获ID: {}", mainOrder.getOrderNo(), resourceId);
                        }
                    } else {
                        OverseasOrder order = overseasOrderMapper.getByPaypalOrderId(paypalOrderId);
                        if (order != null && OverseasOrder.PENDING_PAYMENT.equals(order.getStatus())) {
                            order.setStatus(OverseasOrder.PAID);
                            order.setPaypalCaptureId(resourceId);
                            order.setPayTime(LocalDateTime.now());
                            order.setUpdateTime(LocalDateTime.now());
                            overseasOrderMapper.update(order);
                            log.info("Webhook: 历史海外订单 {} 已更新为已支付，捕获ID: {}", order.getOrderNo(), resourceId);
                        }
                    }
                }
                handled = true;

            } else if ("PAYMENT.CAPTURE.DENIED".equals(eventType)) {
                resourceId = resource != null ? resource.getString("id") : null;
                log.warn("支付被拒绝，捕获ID: {}", resourceId);
                handled = true;

            } else if ("CHECKOUT.ORDER.APPROVED".equals(eventType)) {
                String paypalOrderId = resource != null ? resource.getString("id") : null;
                log.info("订单已审批，PayPal订单ID: {}（等待capture）", paypalOrderId);
                handled = true;

            } else {
                log.info("未处理的事件类型: {}", eventType);
            }

            // 7. ★ 记录事件（幂等）
            if (handled) {
                paypalWebhookEventMapper.insertProcessed(eventId, eventType, resourceId, body);
            }

            response.setStatus(200);
            response.setHeader("Content-type", "application/json");
            response.getOutputStream().write("{\"code\":1,\"message\":\"SUCCESS\"}".getBytes());
            response.flushBuffer();

        } catch (Exception e) {
            log.error("PayPal Webhook回调处理失败", e);
            try {
                response.setStatus(500);
                response.getOutputStream().write("{\"code\":0,\"message\":\"Internal error\"}".getBytes());
                response.flushBuffer();
            } catch (Exception ex) {
                log.error("设置响应状态失败", ex);
            }
        }
    }

    /**
     * 读取请求数据
     */
    private String readData(HttpServletRequest request) throws Exception {
        BufferedReader reader = request.getReader();
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append(line);
        }
        return result.toString();
    }
}
