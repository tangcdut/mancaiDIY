package com.diy.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.diy.properties.WeChatProperties;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/**
 * 微信支付工具类（手动实现公钥验签）
 */
@Component
public class WeChatPayUtil {

    @Autowired
    private WeChatProperties weChatProperties;

    private static final String JSAPI_URL = "https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi";
    private static final String REFUND_URL = "https://api.mch.weixin.qq.com/v3/refund/domestic/refunds";

    /**
     * 加载商户私钥
     */
    private PrivateKey loadPrivateKey() throws Exception {
        String privateKeyPath = weChatProperties.getPrivateKeyFilePath();
        java.nio.file.Path path = Paths.get(privateKeyPath);
        if (!Files.exists(path)) {
            // 如果是在子模块目录运行，尝试往上一级寻找 certs
            java.nio.file.Path parentCertsPath = Paths.get("..").resolve(privateKeyPath);
            if (Files.exists(parentCertsPath)) {
                path = parentCertsPath;
            }
        }
        String privateKeyPEM = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * 加载微信支付公钥
     */
    private PublicKey loadPublicKey() throws Exception {
        String publicKeyPath = weChatProperties.getWeChatPayPublicKeyPath();
        java.nio.file.Path path = Paths.get(publicKeyPath);
        if (!Files.exists(path)) {
            // 如果是在子模块目录运行，尝试往上一级寻找 certs
            java.nio.file.Path parentCertsPath = Paths.get("..").resolve(publicKeyPath);
            if (Files.exists(parentCertsPath)) {
                path = parentCertsPath;
            }
        }
        String publicKeyPEM = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * 生成请求签名（Authorization header）
     */
    private String buildAuthorization(String method, String url, String body) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = UUID.randomUUID().toString().replaceAll("-", "");
        
        // 构建签名串
        String signStr = method + "\n" + url + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        
        // 使用商户私钥签名
        PrivateKey privateKey = loadPrivateKey();
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(privateKey);
        sign.update(signStr.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(sign.sign());
        
        // 构建Authorization header
        return String.format(
            "WECHATPAY2-SHA256-RSA2048 mchid=\"%s\",nonce_str=\"%s\",signature=\"%s\",timestamp=\"%s\",serial_no=\"%s\"",
            weChatProperties.getMchid(),
            nonce,
            signature,
            timestamp,
            weChatProperties.getMchSerialNo()
        );
    }

    /**
     * 验证微信响应签名
     */
    public boolean verifySignature(String timestamp, String nonce, String body, String signature) throws Exception {
        // 构建验签串
        String signStr = timestamp + "\n" + nonce + "\n" + body + "\n";
        
        // 使用微信支付公钥验签
        PublicKey publicKey = loadPublicKey();
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initVerify(publicKey);
        sign.update(signStr.getBytes(StandardCharsets.UTF_8));
        
        return sign.verify(Base64.getDecoder().decode(signature));
    }

    /**
     * jsapi下单
     *
     * @param orderNum    商户订单号
     * @param total       总金额
     * @param description 商品描述
     * @param openid      微信用户的openid
     * @return
     */
    public JSONObject pay(String orderNum, BigDecimal total, String description, String openid) throws Exception {
        try {
            System.out.println("[微信支付] 开始JSAPI下单（手动实现公钥验签）");
            System.out.println("[微信支付] 订单号: " + orderNum);
            System.out.println("[微信支付] 金额: " + total);

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("appid", weChatProperties.getAppid());
            requestBody.put("mchid", weChatProperties.getMchid());
            requestBody.put("description", description);
            requestBody.put("out_trade_no", orderNum);
            requestBody.put("notify_url", weChatProperties.getNotifyUrl());

            JSONObject amount = new JSONObject();
            amount.put("total", total.multiply(new BigDecimal(100)).intValue());
            amount.put("currency", "CNY");
            requestBody.put("amount", amount);

            JSONObject payer = new JSONObject();
            payer.put("openid", openid);
            requestBody.put("payer", payer);

            String body = requestBody.toJSONString();
            System.out.println("[微信支付] 请求体: " + body);

            // 构建HTTP请求
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(JSAPI_URL);
            
            // 设置请求头
            String authorization = buildAuthorization("POST", "/v3/pay/transactions/jsapi", body);
            httpPost.setHeader("Authorization", authorization);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("User-Agent", "DIY-Backend");
            // 添加公钥ID请求头
            httpPost.setHeader("Wechatpay-Serial", weChatProperties.getWeChatPayPublicKeyId());
            
            httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

            // 发送请求
            CloseableHttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            int statusCode = response.getStatusLine().getStatusCode();

            System.out.println("[微信支付] 响应状态码: " + statusCode);
            System.out.println("[微信支付] 响应体: " + responseBody);

            if (statusCode != 200) {
                throw new RuntimeException("微信支付下单失败，状态码: " + statusCode + ", 响应: " + responseBody);
            }

            // 验证响应签名
            String timestamp = response.getFirstHeader("Wechatpay-Timestamp").getValue();
            String nonce = response.getFirstHeader("Wechatpay-Nonce").getValue();
            String signature = response.getFirstHeader("Wechatpay-Signature").getValue();
            
            if (!verifySignature(timestamp, nonce, responseBody, signature)) {
                throw new RuntimeException("微信支付响应签名验证失败");
            }

            // 解析响应
            JSONObject responseJson = JSON.parseObject(responseBody);
            String prepayId = responseJson.getString("prepay_id");

            System.out.println("[微信支付] 下单成功，prepayId: " + prepayId);

            // 构造数据给微信小程序
            String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonceStr = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 32);
            String packageValue = "prepay_id=" + prepayId;

            // 生成小程序调起支付签名
            String message = weChatProperties.getAppid() + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageValue + "\n";
            String paySign = sign(message);

            JSONObject jo = new JSONObject();
            jo.put("timeStamp", timeStamp);
            jo.put("nonceStr", nonceStr);
            jo.put("package", packageValue);
            jo.put("signType", "RSA");
            jo.put("paySign", paySign);

            httpClient.close();
            return jo;
        } catch (Exception e) {
            throw new RuntimeException("微信支付调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用已有的 prepay_id 重新生成小程序调起支付参数。
     *
     * <p>场景：用户在微信支付页取消支付后，允许再次对同一笔【待支付】订单发起支付。
     * 微信侧 prepay_id 在有效期内可复用，但每次调起支付都需要重新生成 nonceStr/timeStamp/paySign。</p>
     *
     * @param prepayId 微信下单返回的 prepay_id（不包含 prepay_id= 前缀）或 package 字段（包含 prepay_id= 前缀）
     */
    public JSONObject buildJsapiPayParamsFromPrepayId(String prepayId) throws Exception {
        if (prepayId == null || prepayId.trim().isEmpty()) {
            throw new IllegalArgumentException("prepayId不能为空");
        }

        String trimmed = prepayId.trim();
        String packageValue = trimmed.startsWith("prepay_id=") ? trimmed : "prepay_id=" + trimmed;

        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 32);

        String message = weChatProperties.getAppid() + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageValue + "\n";
        String paySign = sign(message);

        JSONObject jo = new JSONObject();
        jo.put("timeStamp", timeStamp);
        jo.put("nonceStr", nonceStr);
        jo.put("package", packageValue);
        jo.put("signType", "RSA");
        jo.put("paySign", paySign);
        return jo;
    }

    /**
     * 申请退款
     *
     * @param outTradeNo    商户订单号
     * @param outRefundNo   商户退款单号
     * @param refundAmount  退款金额
     * @param total         原订单金额
     * @return
     */
    public String refund(String outTradeNo, String outRefundNo, BigDecimal refundAmount, BigDecimal total) throws Exception {
        try {
            System.out.println("[微信支付] 开始申请退款（手动实现公钥验签）");
            System.out.println("[微信支付] 订单号: " + outTradeNo);
            System.out.println("[微信支付] 退款金额: " + refundAmount);

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("out_trade_no", outTradeNo);
            requestBody.put("out_refund_no", outRefundNo);
            requestBody.put("notify_url", weChatProperties.getRefundNotifyUrl());

            JSONObject amount = new JSONObject();
            amount.put("refund", refundAmount.multiply(new BigDecimal(100)).longValue());
            amount.put("total", total.multiply(new BigDecimal(100)).longValue());
            amount.put("currency", "CNY");
            requestBody.put("amount", amount);

            String body = requestBody.toJSONString();

            // 构建HTTP请求
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(REFUND_URL);
            
            // 设置请求头
            String authorization = buildAuthorization("POST", "/v3/refund/domestic/refunds", body);
            httpPost.setHeader("Authorization", authorization);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("User-Agent", "DIY-Backend");
            httpPost.setHeader("Wechatpay-Serial", weChatProperties.getWeChatPayPublicKeyId());
            
            httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

            // 发送请求
            CloseableHttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 200) {
                throw new RuntimeException("微信退款失败，状态码: " + statusCode + ", 响应: " + responseBody);
            }

            // 验证响应签名
            String timestamp = response.getFirstHeader("Wechatpay-Timestamp").getValue();
            String nonce = response.getFirstHeader("Wechatpay-Nonce").getValue();
            String signature = response.getFirstHeader("Wechatpay-Signature").getValue();
            
            if (!verifySignature(timestamp, nonce, responseBody, signature)) {
                throw new RuntimeException("微信退款响应签名验证失败");
            }

            System.out.println("[微信支付] 退款成功");

            httpClient.close();
            return responseBody;
        } catch (Exception e) {
            throw new RuntimeException("微信退款调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成签名（用于小程序调起支付）
     *
     * @param message 待签名消息
     * @return 签名字符串
     */
    private String sign(String message) throws Exception {
        try {
            PrivateKey privateKey = loadPrivateKey();
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(privateKey);
            sign.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sign.sign());
        } catch (Exception e) {
            throw new RuntimeException("签名生成失败: " + e.getMessage(), e);
        }
    }
}