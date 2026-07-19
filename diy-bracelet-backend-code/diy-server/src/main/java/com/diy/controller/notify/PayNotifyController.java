package com.diy.controller.notify;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.diy.properties.WeChatProperties;
import com.diy.service.OrderService;
import com.diy.utils.WeChatPayUtil;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * 支付回调相关接口
 */
@RestController
@RequestMapping("/notify")
@Slf4j
@Api(tags = "微信支付回调接口")
public class PayNotifyController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 支付成功回调
     *
     * @param request
     */
    @RequestMapping("/paySuccess")
    public void paySuccessNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            //读取数据
            String body = readData(request);
            log.info("支付成功回调：{}", body);
            
            // 验证签名（使用公钥验签）
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String signature = request.getHeader("Wechatpay-Signature");
            
            log.info("回调签名验证: timestamp={}, nonce={}", timestamp, nonce);
            
            if (!weChatPayUtil.verifySignature(timestamp, nonce, body, signature)) {
                log.error("回调签名验证失败");
                response.setStatus(401);
                return;
            }
            
            log.info("回调签名验证成功");
            
            // 解密通知
            String plainText = decryptNotification(body);
            log.info("解密后的文本：{}", plainText);

            JSONObject jsonObject = JSON.parseObject(plainText);
            String outTradeNo = jsonObject.getString("out_trade_no");//商户平台订单号
            String transactionId = jsonObject.getString("transaction_id");//微信支付交易号

            log.info("商户平台订单号：{}", outTradeNo);
            log.info("微信支付交易号：{}", transactionId);

            //业务处理：修改订单状态、存储微信交易号
            orderService.paySuccess(outTradeNo, transactionId);

            //给微信响应
            responseToWeixin(response);
        } catch (Exception e) {
            log.error("支付回调处理失败", e);
            throw e;
        }
    }

    /**
     * 读取数据
     *
     * @param request
     * @return
     * @throws Exception
     */
    private String readData(HttpServletRequest request) throws Exception {
        BufferedReader reader = request.getReader();
        StringBuilder result = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append(line);
        }
        return result.toString();
    }

    /**
     * 解密通知
     * @param body 请求体
     * @return 解密后的字符串
     */
    private String decryptNotification(String body) throws Exception {
        try {
            // 解析请求体获取加密数据
            JSONObject bodyJson = JSON.parseObject(body);
            JSONObject resource = bodyJson.getJSONObject("resource");
            String ciphertext = resource.getString("ciphertext");
            String nonce = resource.getString("nonce");
            String associatedData = resource.getString("associated_data");

            // 使用AES解密
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                    weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8), "AES");
            javax.crypto.spec.GCMParameterSpec gcmSpec = new javax.crypto.spec.GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            
            byte[] decryptedBytes = cipher.doFinal(java.util.Base64.getDecoder().decode(ciphertext));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密通知失败", e);
            throw new RuntimeException("解密通知失败: " + e.getMessage(), e);
        }
    }

    /**
     * 给微信响应
     * @param response
     */
    private void responseToWeixin(HttpServletResponse response) throws Exception{
        response.setStatus(200);
        HashMap<Object, Object> map = new HashMap<>();
        map.put("code", "SUCCESS");
        map.put("message", "SUCCESS");
        response.setHeader("Content-type", "application/json");
        response.getOutputStream().write(JSON.toJSONString(map).getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }
    
    /**
     * 退款成功回调
     * @param request
     * @param response
     */
    @RequestMapping("/refundSuccess")
    public void refundSuccessNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            // 读取数据
            String body = readData(request);
            log.info("退款成功回调：{}", body);
            
            // 验证签名（使用公钥验签）
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String signature = request.getHeader("Wechatpay-Signature");
            
            log.info("回调签名验证: timestamp={}, nonce={}", timestamp, nonce);
            
            if (!weChatPayUtil.verifySignature(timestamp, nonce, body, signature)) {
                log.error("回调签名验证失败");
                response.setStatus(401);
                return;
            }
            
            log.info("回调签名验证成功");
            
            // 解密通知
            String plainText = decryptNotification(body);
            log.info("解密后的文本：{}", plainText);

            JSONObject jsonObject = JSON.parseObject(plainText);
            String outTradeNo = jsonObject.getString("out_trade_no"); // 商户平台订单号
            String refundStatus = jsonObject.getString("refund_status"); // 退款状态

            log.info("商户平台订单号：{}", outTradeNo);
            log.info("退款状态：{}", refundStatus);
            
            // 只有退款成功时才处理
            if ("SUCCESS".equals(refundStatus)) {
                // 业务处理：修改订单状态、回滚库存
                orderService.refundSuccess(outTradeNo);
            } else {
                log.warn("退款状态非 SUCCESS，不处理，状态: {}", refundStatus);
            }

            // 给微信响应
            responseToWeixin(response);
        } catch (Exception e) {
            log.error("退款回调处理失败", e);
            throw e;
        }
    }
}