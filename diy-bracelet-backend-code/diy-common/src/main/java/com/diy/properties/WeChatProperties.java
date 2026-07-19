package com.diy.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "diy.wechat")
@Data
public class WeChatProperties {

    private String appid; //小程序的appid
    private String secret; //小程序的秘钥
    private String mchid; //商户号
    private String mchSerialNo; //商户API证书的证书序列号
    private String privateKeyFilePath; //商户私钥文件
    private String apiV3Key; //APIv3密钥（32位字符串，用于AES-256-GCM加解密）
    private String weChatPayPublicKeyId; //微信支付公钥ID（新版验签方式，格式：PUB_KEY_ID_xxxxxx）
    private String weChatPayPublicKeyPath; //微信支付公钥文件路径（新版验签方式，推荐使用）
    private String wechatPayCertFilePath; //微信支付平台证书文件路径（用于本地验签）
    private String notifyUrl; //支付成功的回调地址
    private String refundNotifyUrl; //退款成功的回调地址

}