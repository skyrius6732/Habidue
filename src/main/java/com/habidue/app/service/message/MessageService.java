package com.habidue.app.service.message;

import com.habidue.app.domain.message.Message;
import com.habidue.app.domain.message.MessageFile;
import com.habidue.app.domain.message.UserBlock;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.message.AiAnalysisResult;
import com.habidue.app.repository.message.MessageRepository;
import com.habidue.app.repository.message.UserBlockRepository;
import com.habidue.app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final List<MessageAnalyzer> analyzers;

    @Value("${file.upload-dir:/home/skyrius/habiDue/uploads/messages}")
    private String uploadDir;

    /**
     * 유저 간 쪽지 발송 (파일 첨부 지원)
     */
    @Transactional
    public void sendMessage(Long senderId, Long receiverId, String content, List<MultipartFile> files) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new NoSuchElementException("발신자를 찾을 수 없습니다."));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new NoSuchElementException("수신자를 찾을 수 없습니다."));

        if (sender.getKarmaPoint() < 90) {
            throw new IllegalStateException("활동 신뢰 점수가 낮아 쪽지를 보낼 수 없습니다. (최소 90P 필요)");
        }

        if (userBlockRepository.existsByBlockerAndBlocked(receiver, sender)) {
            throw new IllegalStateException("상대방이 쪽지 수신을 원하지 않습니다.");
        }

        AiAnalysisResult finalResult = AiAnalysisResult.safe();
        for (MessageAnalyzer analyzer : analyzers) {
            AiAnalysisResult res = analyzer.analyze(content);
            if (res.getScore() > finalResult.getScore()) {
                finalResult = res;
            }
        }

        if (finalResult.isShouldBlock()) {
            throw new IllegalArgumentException("부적절한 내용이 포함되어 쪽지를 보낼 수 없습니다: " + finalResult.getReason());
        }

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .aiScore(finalResult.getScore())
                .aiAnalysis(finalResult.getReason())
                .build();
        
        // 파일 저장 처리
        if (files != null && !files.isEmpty()) {
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String originalName = file.getOriginalFilename();
                String ext = originalName.substring(originalName.lastIndexOf("."));
                String savedName = UUID.randomUUID().toString() + ext;
                Path filePath = Paths.get(uploadDir, savedName);

                try {
                    Files.copy(file.getInputStream(), filePath);
                    
                    MessageFile messageFile = MessageFile.builder()
                            .message(message)
                            .fileUrl("/uploads/messages/" + savedName)
                            .originalFileName(originalName)
                            .fileType(file.getContentType())
                            .fileSize(file.getSize())
                            .build();
                    
                    message.addAttachment(messageFile);
                } catch (IOException e) {
                    log.error("쪽지 파일 저장 실패: {}", originalName, e);
                }
            }
        }
        
        messageRepository.save(message);
    }

    /**
     * 운영자용 시스템 메시지 발송
     */
    @Transactional
    public void sendSystemMessage(Long receiverId, String content) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new NoSuchElementException("수신자를 찾을 수 없습니다."));

        Message message = Message.builder()
                .receiver(receiver)
                .content(content)
                .isSystem(true)
                .build();
        
        messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public Page<Message> getReceivedMessages(User user, Pageable pageable) {
        return messageRepository.findByReceiverAndDeletedByReceiverFalseOrderByCreatedAtDesc(user, pageable);
    }

    @Transactional
    public void readMessage(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("쪽지를 찾을 수 없습니다."));
        
        if (!message.getReceiver().getId().equals(user.getId())) {
            throw new SecurityException("권한이 없습니다.");
        }
        
        message.markAsRead();
    }

    @Transactional
    public void reportMessage(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("쪽지를 찾을 수 없습니다."));
        
        if (!message.getReceiver().getId().equals(user.getId())) {
            throw new SecurityException("권한이 없습니다.");
        }
        
        message.report();
    }

    @Transactional
    public void blockUser(User blocker, Long blockedId) {
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new NoSuchElementException("차단할 유저를 찾을 수 없습니다."));
        
        if (userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            return;
        }

        userBlockRepository.save(UserBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build());
    }
}
