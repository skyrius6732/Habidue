package com.habidue.app.service.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}")
    private String mailUsername;

    public void sendHtmlMail(String to, String subject, String templateName, Map<String, Object> variables) {
        if (mailUsername == null || mailUsername.isEmpty()) {
            log.error("메일 발송 중단: spring.mail.username 설정이 비어있습니다. 환경 변수를 확인하세요.");
            return;
        }
        
        // 환경 변수 주입 확인용 로그 (앞 2글자만 노출)
        String maskedUser = mailUsername.length() > 2 ? mailUsername.substring(0, 2) + "***" : "***";
        log.info("[MailService] 메일 발송 시도 (발신자: {})", maskedUser);
        
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("HabiDue <" + mailUsername + ">");

            Context context = new Context();
            context.setVariables(variables);
            String html = templateEngine.process("mail/" + templateName, context);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("메일 발송 성공: {}", to);
        } catch (MessagingException e) {
            log.error("메일 발송 실패: {}", to, e);
        }
    }
}
