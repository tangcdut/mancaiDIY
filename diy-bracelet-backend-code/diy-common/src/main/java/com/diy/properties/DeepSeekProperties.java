package com.diy.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "diy.deepseek")
@Data
public class DeepSeekProperties {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * API 基础路径
     */
    private String apiUrl = "https://api.deepseek.com/v1";

    /**
     * 模型名称
     */
    private String model = "deepseek-chat";

    /**
     * 读取超时时间（毫秒），建议设置长一点，AI响应较慢
     */
    private Integer readTimeout = 60000;

    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectTimeout = 10000;
}
