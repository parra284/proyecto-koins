package com.finapp.transactions.infrastructure.config;

import com.finapp.transactions.adapter.output.metrics.TransactionMetricsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration — registers infrastructure interceptors.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TransactionMetricsInterceptor metricsInterceptor;

    public WebConfig(TransactionMetricsInterceptor metricsInterceptor) {
        this.metricsInterceptor = metricsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(metricsInterceptor)
                .addPathPatterns("/api/**");
    }
}
