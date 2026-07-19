package com.diy.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "diy.paypal")
@Data
public class PayPalProperties {

    private String clientId; // PayPal客户端ID
    private String clientSecret; // PayPal客户端密钥
    private String mode; // 模式：sandbox或live
    private String returnUrl; // 支付成功后返回URL
    private String cancelUrl; // 支付取消后返回URL
    private String webhookId; // Webhook ID（用于验证回调）

}
