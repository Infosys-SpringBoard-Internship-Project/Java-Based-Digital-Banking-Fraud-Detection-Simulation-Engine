package com.example.infosys_project.config;

import com.example.infosys_project.interceptor.ApiLogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiLogInterceptor apiLogInterceptor;

    public WebConfig(ApiLogInterceptor apiLogInterceptor) {
        this.apiLogInterceptor = apiLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiLogInterceptor);
    }
}
