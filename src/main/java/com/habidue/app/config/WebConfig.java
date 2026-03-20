package com.habidue.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 프로젝트 루트 경로를 기준으로 uploads 폴더를 지정
        String userHome = System.getProperty("user.home");
        String uploadPath = "file:" + userHome + "/habiDue/uploads/";

        // "/uploads/**" 요청이 들어오면 실제 "/home/skyrius/habiDue/uploads/" 폴더 안을 뒤지도록 설정
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath)
                .setCachePeriod(3600); // 이미지 성능을 위해 1시간 캐싱
    }
}
