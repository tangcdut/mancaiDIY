package com.diy.config;

import com.diy.interceptor.JwtTokenAdminInterceptor;
import com.diy.interceptor.JwtTokenUserInterceptor;
import com.diy.json.JacksonObjectMapper;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;
    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login")
                .excludePathPatterns("/admin/common/image/**");

        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns("/user/user/login")
                .excludePathPatterns("/user/shop/status")
                // DIY设计台查询接口无需登录
                .excludePathPatterns("/user/design/category/list")
                .excludePathPatterns("/user/design/colorSeries/list")
                .excludePathPatterns("/user/design/material/list")
                // 浏览类接口无需登录
                .excludePathPatterns("/user/category/**")
                .excludePathPatterns("/user/product/**")
                .excludePathPatterns("/user/banner/**")
                // 客服二维码公开接口
                .excludePathPatterns("/user/common/customer-service-qr")
                .excludePathPatterns("/notify/paySuccess")
                .excludePathPatterns("/notify/refundSuccess")
                // AI推荐接口无需登录
                .excludePathPatterns("/user/ai/**")
                // 海外版免登录接口
                .excludePathPatterns("/guest/**");
    }

    /**
     * 设置静态资源映射
     * @param registry
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射...");
        // Knife4j API文档
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        
        // 静态资源访问配置
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/ui/**").addResourceLocations("classpath:/static/ui/");
        registry.addResourceHandler("/banner/**").addResourceLocations("classpath:/static/banner/");
        registry.addResourceHandler("/tabbar/**").addResourceLocations("classpath:/static/tabbar/");
        registry.addResourceHandler("/customer-service/**").addResourceLocations("classpath:/static/customer-service/");
    }

    /**
     * 通过knife4j生成接口文档
     * @return
     */
    @Bean
    public Docket docket() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("手链DIY项目接口文档")
                .version("1.0")
                .description("手链DIY项目接口文档")
                .build();
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.diy.controller"))
                .paths(PathSelectors.any())
                .build();
        return docket;
    }
}