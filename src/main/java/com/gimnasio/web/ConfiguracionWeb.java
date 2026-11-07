package com.gimnasio.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new AutenticacionInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/css/**", "/js/**", "/img/**", "/fonts/**", "/favicon.ico", "/error");
    }
}
