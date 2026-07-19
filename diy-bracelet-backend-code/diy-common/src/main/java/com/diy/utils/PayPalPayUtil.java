package com.diy.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.diy.properties.PayPalProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class PayPalPayUtil {

    private static final int TIMEOUT_MSEC = 30 * 1000;

    @Autowired
    private PayPalProperties payPalProperties;

    private String accessToken;
    private long tokenExpireTime;

    /**
     * PayPal创建订单返回结果
     */
    @Data
    public static class CreateOrderResult {
        private String orderId;
        private String approvalUrl;  // 用户需要跳转的PayPal审批页面URL
    }

    private String getBaseUrl() {
        if ("live".equals(payPalProperties.getMode())) {
            return "https://api-m.paypal.com";
        }
        return "https://api-m.sandbox.paypal.com";
    }

    private synchronized String getAccessToken() throws IOException {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return accessToken;
        }

        log.info("[PayPal支付] 获取Access Token");

        String url = getBaseUrl() + "/v1/oauth2/token";
        String auth = java.util.Base64.getEncoder().encodeToString(
                (payPalProperties.getClientId() + ":" + payPalProperties.getClientSecret())
                        .getBytes(StandardCharsets.UTF_8));

        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Basic " + auth);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setConfig(buildRequestConfig());

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("grant_type", "client_credentials"));
            httpPost.setEntity(new UrlEncodedFormEntity(params));

            CloseableHttpResponse response = httpClient.execute(httpPost);
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            int statusCode = response.getStatusLine().getStatusCode();
            response.close();

            if (statusCode != 200) {
                log.error("[PayPal支付] 获取Access Token失败，状态码: {}，响应: {}", statusCode, body);
                throw new RuntimeException("获取PayPal Access Token失败，状态码: " + statusCode);
            }

            JSONObject json = JSONObject.parseObject(body);
            accessToken = json.getString("access_token");
            int expiresIn = json.getIntValue("expires_in");
            tokenExpireTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L;

            log.info("[PayPal支付] 获取Access Token成功，有效期: {}秒", expiresIn);
            return accessToken;
        } finally {
            httpClient.close();
        }
    }

    /**
     * 创建PayPal订单
     * @return CreateOrderResult 包含 orderId 和 approvalUrl（用户跳转PayPal审批页面的链接）
     */
    public CreateOrderResult createOrder(String orderNo, BigDecimal total, String description, String currencyCode) throws IOException {
        log.info("[PayPal支付] 开始创建订单，订单号: {}，金额: {} {}", orderNo, total, currencyCode);

        String url = getBaseUrl() + "/v2/checkout/orders";
        String token = getAccessToken();

        JSONObject requestBody = new JSONObject();
        requestBody.put("intent", "CAPTURE");

        JSONObject amount = new JSONObject();
        amount.put("currency_code", currencyCode);
        amount.put("value", total.toString());

        JSONObject purchaseUnit = new JSONObject();
        purchaseUnit.put("reference_id", orderNo);
        purchaseUnit.put("description", description);
        purchaseUnit.put("amount", amount);
        requestBody.put("purchase_units", new Object[]{purchaseUnit});

        JSONObject applicationContext = new JSONObject();
        applicationContext.put("brand_name", "Bracelet Shop");
        applicationContext.put("locale", "en-US");
        applicationContext.put("landing_page", "BILLING");
        applicationContext.put("shipping_preference", "NO_SHIPPING");
        applicationContext.put("user_action", "PAY_NOW");
        applicationContext.put("return_url", payPalProperties.getReturnUrl());
        applicationContext.put("cancel_url", payPalProperties.getCancelUrl());
        requestBody.put("application_context", applicationContext);

        String body = requestBody.toJSONString();
        log.info("[PayPal支付] 请求体: {}", body);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + token);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Prefer", "return=representation");
            httpPost.setConfig(buildRequestConfig());
            httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

            CloseableHttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            int statusCode = response.getStatusLine().getStatusCode();
            response.close();

            log.info("[PayPal支付] 响应状态码: {}，响应体: {}", statusCode, responseBody);

            if (statusCode != 201) {
                log.error("[PayPal支付] 订单创建失败，状态码: {}，响应: {}", statusCode, responseBody);
                throw new RuntimeException("PayPal订单创建失败，状态码: " + statusCode + "，响应: " + responseBody);
            }

            JSONObject responseJson = JSONObject.parseObject(responseBody);
            String paypalOrderId = responseJson.getString("id");

            // ★ 关键修复：从 PayPal 响应的 links 中提取 approval URL（用户跳转PayPal审批页面的链接）
            String approvalUrl = null;
            JSONArray links = responseJson.getJSONArray("links");
            if (links != null) {
                for (int i = 0; i < links.size(); i++) {
                    JSONObject link = links.getJSONObject(i);
                    if ("approve".equals(link.getString("rel"))) {
                        approvalUrl = link.getString("href");
                        break;
                    }
                }
            }

            log.info("[PayPal支付] 订单创建成功，PayPal订单ID: {}，审批URL: {}", paypalOrderId, approvalUrl);

            CreateOrderResult result = new CreateOrderResult();
            result.setOrderId(paypalOrderId);
            result.setApprovalUrl(approvalUrl);
            return result;
        } finally {
            httpClient.close();
        }
    }

    /**
     * 捕获PayPal订单（带幂等键）
     * @param paypalOrderId PayPal订单ID
     * @return 捕获ID
     */
    public String captureOrder(String paypalOrderId) throws IOException {
        log.info("[PayPal支付] 开始捕获订单，PayPal订单ID: {}", paypalOrderId);

        String url = getBaseUrl() + "/v2/checkout/orders/" + paypalOrderId + "/capture";
        String token = getAccessToken();

        // ★ 生成幂等键，防止网络重试导致重复扣款
        String requestId = UUID.randomUUID().toString();

        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + token);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Prefer", "return=representation");
            httpPost.setHeader("PayPal-Request-Id", requestId);
            httpPost.setConfig(buildRequestConfig());
            httpPost.setEntity(new StringEntity("{}", StandardCharsets.UTF_8));

            CloseableHttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            int statusCode = response.getStatusLine().getStatusCode();
            response.close();

            if (statusCode != 200 && statusCode != 201) {
                log.error("[PayPal支付] 订单捕获失败，状态码: {}，响应: {}", statusCode, responseBody);
                throw new RuntimeException("PayPal订单捕获失败，状态码: " + statusCode + "，响应: " + responseBody);
            }

            JSONObject responseJson = JSONObject.parseObject(responseBody);
            String status = responseJson.getString("status");
            String captureId = extractCaptureId(responseJson);
            log.info("[PayPal支付] 订单捕获成功，状态: {}，捕获ID: {}", status, captureId);
            return captureId;
        } finally {
            httpClient.close();
        }
    }

    public JSONObject getOrderDetails(String paypalOrderId) throws IOException {
        log.info("[PayPal支付] 获取订单详情，PayPal订单ID: {}", paypalOrderId);

        String url = getBaseUrl() + "/v2/checkout/orders/" + paypalOrderId;
        String token = getAccessToken();

        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", "Bearer " + token);
            httpGet.setHeader("Content-Type", "application/json");
            httpGet.setConfig(buildRequestConfig());

            CloseableHttpResponse response = httpClient.execute(httpGet);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            int statusCode = response.getStatusLine().getStatusCode();
            response.close();

            if (statusCode != 200) {
                log.error("[PayPal支付] 获取订单详情失败，状态码: {}，响应: {}", statusCode, responseBody);
                throw new RuntimeException("获取PayPal订单详情失败，状态码: " + statusCode);
            }

            return JSONObject.parseObject(responseBody);
        } finally {
            httpClient.close();
        }
    }

    public String extractCaptureId(JSONObject orderJson) {
        if (orderJson == null) {
            return null;
        }

        JSONArray purchaseUnits = orderJson.getJSONArray("purchase_units");
        if (purchaseUnits == null) {
            return null;
        }

        for (int i = 0; i < purchaseUnits.size(); i++) {
            JSONObject unit = purchaseUnits.getJSONObject(i);
            JSONObject payments = unit.getJSONObject("payments");
            if (payments == null) {
                continue;
            }
            JSONArray captures = payments.getJSONArray("captures");
            if (captures != null && !captures.isEmpty()) {
                return captures.getJSONObject(0).getString("id");
            }
        }
        return null;
    }

    /**
     * ★ 验证PayPal Webhook签名
     * 向 PayPal API 发送验证请求，防止伪造回调
     *
     * @param authAlgo         PAYPAL-AUTH-ALGO header
     * @param certUrl          PAYPAL-CERT-URL header
     * @param transmissionId   PAYPAL-TRANSMISSION-ID header
     * @param transmissionSig  PAYPAL-TRANSMISSION-SIG header
     * @param transmissionTime PAYPAL-TRANSMISSION-TIME header
     * @param webhookEventBody Webhook POST Body 原始JSON字符串
     * @return 验证是否通过
     */
    public boolean verifyWebhookSignature(String authAlgo, String certUrl,
                                          String transmissionId, String transmissionSig,
                                          String transmissionTime, String webhookEventBody) {
        log.info("[PayPal Webhook] 开始验证签名，transmissionId: {}", transmissionId);

        String url = getBaseUrl() + "/v1/notifications/verify-webhook-signature";
        String token;
        try {
            token = getAccessToken();
        } catch (IOException e) {
            log.error("[PayPal Webhook] 获取Access Token失败", e);
            return false;
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("auth_algo", authAlgo);
        requestBody.put("cert_url", certUrl);
        requestBody.put("transmission_id", transmissionId);
        requestBody.put("transmission_sig", transmissionSig);
        requestBody.put("transmission_time", transmissionTime);
        requestBody.put("webhook_event", JSONObject.parseObject(webhookEventBody));
        requestBody.put("webhook_id", payPalProperties.getWebhookId());

        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + token);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setConfig(buildRequestConfig());
            httpPost.setEntity(new StringEntity(requestBody.toJSONString(), StandardCharsets.UTF_8));

            CloseableHttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            int statusCode = response.getStatusLine().getStatusCode();
            response.close();

            log.info("[PayPal Webhook] 签名验证响应，状态码: {}，响应: {}", statusCode, responseBody);

            if (statusCode == 200) {
                JSONObject respJson = JSONObject.parseObject(responseBody);
                String verificationStatus = respJson.getString("verification_status");
                boolean success = "SUCCESS".equals(verificationStatus);
                log.info("[PayPal Webhook] 签名验证结果: {}", success ? "通过" : "失败");
                return success;
            }
            return false;
        } catch (Exception e) {
            log.error("[PayPal Webhook] 签名验证异常", e);
            return false;
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                log.warn("[PayPal Webhook] 关闭HTTP连接异常", e);
            }
        }
    }

    /**
     * 退款 PayPal 订单
     * @param paypalCaptureId PayPal捕获ID
     * @param amount 退款金额
     * @param currencyCode 货币代码（如USD、HKD）
     * @return 退款ID
     */
    public String refund(String paypalCaptureId, BigDecimal amount, String currencyCode) throws IOException {
        log.info("[PayPal支付] 开始退款，捕获ID: {}，金额: {} {}", paypalCaptureId, amount, currencyCode);

        String url = getBaseUrl() + "/v2/payments/captures/" + paypalCaptureId + "/refund";
        String token = getAccessToken();

        JSONObject requestBody = new JSONObject();
        JSONObject amountJson = new JSONObject();
        amountJson.put("value", amount.toString());
        amountJson.put("currency_code", currencyCode);
        requestBody.put("amount", amountJson);
        requestBody.put("note_to_payer", "Refund for order");

        String body = requestBody.toJSONString();
        String requestId = UUID.randomUUID().toString();

        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer " + token);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("PayPal-Request-Id", requestId);
            httpPost.setConfig(buildRequestConfig());
            httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

            CloseableHttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            int statusCode = response.getStatusLine().getStatusCode();
            response.close();

            log.info("[PayPal支付] 退款响应状态码: {}，响应体: {}", statusCode, responseBody);

            if (statusCode != 201 && statusCode != 200) {
                log.error("[PayPal支付] 退款失败，状态码: {}，响应: {}", statusCode, responseBody);
                throw new RuntimeException("PayPal退款失败，状态码: " + statusCode + "，响应: " + responseBody);
            }

            JSONObject responseJson = JSONObject.parseObject(responseBody);
            String refundId = responseJson.getString("id");
            log.info("[PayPal支付] 退款成功，退款ID: {}", refundId);
            return refundId;
        } finally {
            httpClient.close();
        }
    }

    private RequestConfig buildRequestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(TIMEOUT_MSEC)
                .setConnectionRequestTimeout(TIMEOUT_MSEC)
                .setSocketTimeout(TIMEOUT_MSEC)
                .build();
    }
}
