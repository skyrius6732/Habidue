package com.habidue.app.service.message;

import com.habidue.app.domain.message.Message;
import com.habidue.app.domain.message.MessageFile;
import com.habidue.app.domain.message.UserBlock;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.KarmaReason;
import com.habidue.app.dto.message.AiAnalysisResult;
import com.habidue.app.repository.message.MessageRepository;
import com.habidue.app.repository.message.UserBlockRepository;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.service.user.KarmaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final List<MessageAnalyzer> analyzers;
    private final KarmaService karmaService;
    private final StringRedisTemplate redisTemplate;

    private static final String DAILY_MESSAGE_COUNT_KEY = "message:daily:count:";
    private static final int MAX_DAILY_MESSAGES = 20;

    @Value("${file.upload-dir:/home/skyrius/habiDue/uploads/messages}")
    private String uploadDir;

    /**
     * 유저 간 쪽지 발송 (발송 제한 체크 추가)
     */
    @Transactional
    public void sendMessage(Long senderId, Long receiverId, String content, List<MultipartFile> files) {
        if (receiverId == null) {
            throw new IllegalArgumentException("수신자 ID가 누락되었습니다.");
        }
        
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new NoSuchElementException("발신자를 찾을 수 없습니다."));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new NoSuchElementException("수신자를 찾을 수 없습니다."));

        // [시니어 조치] 해당 대화방이 관리자에 의해 영구 제한되었는지 확인
        // 대화방 식별을 위해 두 사람 간의 최신 메시지 중 isRoomRestricted가 true인 것이 있는지 확인
        boolean isRestricted = messageRepository.existsRestrictedMessageBetweenUsers(sender, receiver);
        if (isRestricted) {
            throw new IllegalStateException("해당 대화방은 운영원칙 위반으로 인해 메시지 발송이 영구 제한되었습니다.");
        }

        // [시니어 조치] 일일 쪽지 발송 횟수 제한 (20회)
        String todayKey = DAILY_MESSAGE_COUNT_KEY + senderId + ":" + LocalDateTime.now().toLocalDate();
        String countStr = redisTemplate.opsForValue().get(todayKey);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;

        if (currentCount >= MAX_DAILY_MESSAGES) {
            throw new IllegalStateException("일일 쪽지 발송 제한(20회)을 초과했습니다. 내일 다시 이용해 주세요.");
        }

        // [시니어 조치] 카르마 시스템 연동
        if (sender.getKarmaPoint() < 800) {
            throw new IllegalStateException("신뢰 점수(Karma)가 낮아 쪽지 발송이 제한되었습니다.");
        }

        if (userBlockRepository.existsByBlockerAndBlocked(receiver, sender)) {
            throw new IllegalStateException("현재 쪽지를 보낼 수 없는 상대입니다.");
        }

        // [사용자 요청] AI 분석 로직 제거 (자유로운 소통 허용)
        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(content) 
                .build();
        
        if (files != null && !files.isEmpty()) {
            saveFiles(message, files);
        }
        
        messageRepository.save(message);

        // 카르마 차감
        karmaService.manualAdjustKarmaRaw(senderId, -1, KarmaReason.MESSAGE_SENT, "쪽지 발송 비용 소모", null, true);

        // 발송 횟수 증가
        redisTemplate.opsForValue().increment(todayKey);
        redisTemplate.expire(todayKey, 1, TimeUnit.DAYS);
    }

    /**
     * 대화방 전체 삭제 (사용자 화면에서만 안 보이게 처리)
     */
    @Transactional
    public void deleteConversation(User user, Long partnerId) {
        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> new NoSuchElementException("상대방을 찾을 수 없습니다."));

        List<Message> messages = messageRepository.findConversationWithPartner(user, partner);
        
        for (Message msg : messages) {
            if (msg.getSender().getId().equals(user.getId())) {
                msg.deleteBySender();
            } else if (msg.getReceiver().getId().equals(user.getId())) {
                msg.deleteByReceiver();
            }
        }
    }

    /**
     * 시스템 메시지 발송 (관리자용 - 제한 없음)
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

    /**
     * 관리자 신고 처리 결과 시스템 메시지 발송 (특수 플래그 및 연관 ID 포함)
     */
    @Transactional
    public void sendAdminResultSystemMessage(User sender, User receiver, String content, Long visibleToUserId, boolean isRoomRestricted, Long relatedTargetId) {
        // [시니어 조치] 같은 타겟(신고건)에 대해 이미 해당 유저에게 보낸 시스템 메시지가 있는지 확인
        if (relatedTargetId != null && visibleToUserId != null) {
            java.util.Optional<Message> existingMsg = messageRepository.findByIsSystemTrueAndRelatedTargetIdAndVisibleToUserId(relatedTargetId, visibleToUserId);
            if (existingMsg.isPresent()) {
                // 이미 존재하면 내용과 제한 플래그만 업데이트 (Upsert 방식)
                existingMsg.get().updateSystemMessage(content, isRoomRestricted);
                return; // 저장 없이 종료 (JPA 영속성 컨텍스트에 의해 변경 감지)
            }
        }

        // 존재하지 않을 경우에만 새로 생성
        Message message = Message.builder()
                .sender(sender) 
                .receiver(receiver)
                .content(content)
                .isSystem(true)
                .visibleToUserId(visibleToUserId)
                .isRoomRestricted(isRoomRestricted)
                .relatedTargetId(relatedTargetId)
                .build();
        
        messageRepository.save(message);
    }

    /**
     * [시니어 조치] 특정 신고 건(relatedTargetId)으로 인해 발생한 모든 시스템 메시지를 일괄 삭제 (복구/Undo 용도)
     */
    @Transactional
    public void clearSystemMessages(Long relatedTargetId) {
        if (relatedTargetId == null) return;
        List<Message> oldSystemMessages = messageRepository.findAllByIsSystemTrueAndRelatedTargetId(relatedTargetId);
        for (Message oldMsg : oldSystemMessages) {
            oldMsg.softDelete();
        }
    }

    /**
     * 관리자에 의한 메시지 복구
     */
    @Transactional
    public void restoreMessage(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("쪽지를 찾을 수 없습니다."));
        
        // isDeleted 플래그 해제 및 복구 표식 설정
        message.restore(); // 엔티티에 메서드 추가 필요
    }

    private void saveFiles(Message message, List<MultipartFile> files) {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalName = file.getOriginalFilename();
            if (originalName == null) continue;
            
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

    @Transactional(readOnly = true)
    public Page<Message> getLatestMessagesGroupedByPartner(User user, Pageable pageable) {
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        Page<Message> latestMessages = messageRepository.findLatestMessagesByPartnersWithTime(user, ninetyDaysAgo, pageable);
        
        // 각 메시지에 대해 상대방이 보낸 읽지 않은 메시지 수 설정
        // 이 부분은 MessageResponseDto 변환 시 처리하거나, 여기서 필드에 세팅 가능
        // 현재 Message 엔티티에 unreadCount 필드가 없으므로 DTO 변환 시 직접 계산하도록 함
        return latestMessages;
    }

    @Transactional(readOnly = true)
    public long countUnreadMessagesWithPartner(User user, User partner) {
        return messageRepository.countUnreadMessagesWithPartner(user, partner);
    }

    /**
     * 개별 쪽지 삭제 (양쪽 모두에게 삭제 표시, 원본 DB 보존)
     */
    @Transactional
    public void deleteMessage(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("쪽지를 찾을 수 없습니다."));
        
        // 발신자만 삭제 가능하도록 제한
        if (!message.getSender().getId().equals(user.getId())) {
            throw new SecurityException("본인이 작성한 메시지만 삭제할 수 있습니다.");
        }
        
        message.softDelete();
    }

    /**
     * 개별 쪽지 수정
     */
    @Transactional
    public void editMessage(Long messageId, User user, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("쪽지를 찾을 수 없습니다."));
        
        if (!message.getSender().getId().equals(user.getId())) {
            throw new SecurityException("본인이 작성한 메시지만 수정할 수 있습니다.");
        }

        if (message.isDeleted()) {
            throw new IllegalStateException("삭제된 메시지는 수정할 수 없습니다.");
        }
        
        message.updateContent(newContent);
    }

    @Transactional
    public List<Message> getConversation(User user, Long partnerId) {
        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> new NoSuchElementException("상대방을 찾을 수 없습니다."));
        
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        List<Message> conversation = messageRepository.findConversationWithPartnerWithTime(user, partner, ninetyDaysAgo);
        
        conversation.stream()
                .filter(m -> m.getReceiver().getId().equals(user.getId()) && !m.isRead())
                .forEach(Message::markAsRead);
                
        return conversation;
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getDailyMessageStatus(Long userId) {
        String todayKey = DAILY_MESSAGE_COUNT_KEY + userId + ":" + LocalDateTime.now().toLocalDate();
        String countStr = redisTemplate.opsForValue().get(todayKey);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
        
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("currentCount", currentCount);
        status.put("maxCount", MAX_DAILY_MESSAGES);
        status.put("remainingCount", Math.max(0, MAX_DAILY_MESSAGES - currentCount));
        return status;
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
        
        // 해당 대화의 참여자인지 확인
        boolean isParticipant = message.getSender().getId().equals(user.getId()) || 
                                message.getReceiver().getId().equals(user.getId());
        
        if (!isParticipant) {
            throw new SecurityException("해당 메시지에 대한 신고 권한이 없습니다.");
        }
        
        message.report();
    }

    @Transactional
    public void blockUser(User blocker, Long blockedId, String reason, boolean isSystemBlock) {
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new NoSuchElementException("차단할 유저를 찾을 수 없습니다."));

        if (userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            return;
        }

        userBlockRepository.save(UserBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .reason(reason)
                .isSystemBlock(isSystemBlock)
                .build());
    }

    @Transactional
    public void blockUser(User blocker, Long blockedId) {
        blockUser(blocker, blockedId, "사용자 수동 차단", false);
    }
    /**
     * 차단 해제
     */
    @Transactional
    public void unblockUser(User blocker, Long blockedId) {
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new NoSuchElementException("유저를 찾을 수 없습니다."));

        userBlockRepository.deleteByBlockerAndBlocked(blocker, blocked);

        // [시니어 조치] 차단 해제 시 해당 유저와의 대화방에 걸린 영구 제한도 함께 해제
        messageRepository.clearRestrictionBetweenUsers(blocker, blocked);
    }
    /**
     * 차단한 유저 목록 조회
     */
    @Transactional(readOnly = true)
    public List<UserBlock> getBlockedUsers(User blocker) {
        return userBlockRepository.findAllByBlocker(blocker);
    }
}
