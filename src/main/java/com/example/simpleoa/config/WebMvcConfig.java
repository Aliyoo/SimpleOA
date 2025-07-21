package com.example.simpleoa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // CORS配置已移至SecurityConfig.java中统一管理
    // 避免与Spring Security的CORS配置冲突
}
