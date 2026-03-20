package com.habidue.app.service.mail;

import com.habidue.app.domain.user.User;
import com.habidue.app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReportEmailService {

    private final StringRedisTemplate redisTemplate; // Redis 사용
    private final MailService mailService;
    private final UserRepository userRepository;

    private static final String VERIFICATION_PREFIX = "mail_verify:";

    @Transactional
    public void sendVerificationCode(User user, String email) {
        String code = String.format("%06d", new Random().nextInt(1000000));
        
        // 1. Redis에 저장 (Key: mail_verify:이메일, Value: 코드, TTL: 10분)
        redisTemplate.opsForValue().set(
                VERIFICATION_PREFIX + email, 
                code, 
                10, 
                TimeUnit.MINUTES
        );

        Map<String, Object> variables = new HashMap<>();
        variables.put("code", code);
        variables.put("username", user.getUsername());

        mailService.sendHtmlMail(email, "[HabiDue] 이메일 인증 코드가 도착했습니다.", "verification", variables);
    }

    @Transactional
    public boolean verifyCode(User user, String email, String code) {
        // 2. Redis에서 코드 조회
        String savedCode = redisTemplate.opsForValue().get(VERIFICATION_PREFIX + email);

        if (savedCode == null || !savedCode.equals(code)) {
            return false;
        }

        // 인증 성공 시 Redis 데이터 삭제 (일회용)
        redisTemplate.delete(VERIFICATION_PREFIX + email);

        user.setReportEmail(email);
        user.setReportEmailVerified(true);
        userRepository.save(user);
        return true;
    }
}
