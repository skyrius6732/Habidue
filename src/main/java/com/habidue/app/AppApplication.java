package com.habidue.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing // JPA Auditing 활성화 (createdAt, updatedAt 자동 기록)
@EnableScheduling // 스케줄링 활성화
@org.springframework.scheduling.annotation.EnableAsync // 비동기 처리 활성화
@EnableRedisRepositories(basePackages = "com.habidue.app.repository.redis") // Redis 스캔 범위 한정 (RedisHash 미사용)
@SpringBootApplication
public class AppApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppApplication.class, args);
	}

}
