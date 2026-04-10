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
import com.habidue.app.service.storage.FileStorageService;
import com.habidue.app.service.user.KarmaService;
import com.habidue.app.service.notification.NotificationService;
import com.habidue.app.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final KarmaService karmaService;
    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;

    private static final String DAILY_MESSAGE_COUNT_KEY = "message:daily:count:";
    private static final int MAX_DAILY_MESSAGES = 20;

    private final FileStorageService fileStorageService;

    @Transactional
    public void sendMessage(Long senderId, Long receiverId, String content, List<MultipartFile> files) {
        User sender = userRepository.findById(senderId).orElseThrow();
        User receiver = userRepository.findById(receiverId).orElseThrow();

        if (messageRepository.existsRestrictedMessageBetweenUsers(sender, receiver)) {
            throw new IllegalStateException("해당 대화방은 운영원칙 위반으로 인해 메시지 발송이 영구 제한되었습니다.");
        }

        String todayKey = DAILY_MESSAGE_COUNT_KEY + senderId + ":" + LocalDateTime.now().toLocalDate();
        String countStr = redisTemplate.opsForValue().get(todayKey);
        if (countStr != null && Integer.parseInt(countStr) >= MAX_DAILY_MESSAGES) {
            throw new IllegalStateException("일일 쪽지 발송 제한(20회)을 초과했습니다.");
        }

        if (sender.getKarmaPoint() < 800) {
            throw new IllegalStateException("신뢰 점수(Karma)가 낮아 쪽지 발송이 제한되었습니다.");
        }

        Message message = Message.builder().sender(sender).receiver(receiver).content(content).build();
        if (files != null && !files.isEmpty()) saveFiles(message, files);
        messageRepository.save(message);

        notificationService.send(receiver, NotificationType.MESSAGE, String.format("💌 '%s'님으로부터 새로운 쪽지가 도착했습니다.", sender.getNickname()), sender.getId(), null);
        karmaService.manualAdjustKarmaRaw(senderId, -1, KarmaReason.MESSAGE_SENT, "쪽지 발송 비용 소모", null, true);
        redisTemplate.opsForValue().increment(todayKey);
        redisTemplate.expire(todayKey, 1, TimeUnit.DAYS);
    }

    /**
     * 컨트롤러에서 요구하는 메서드명 보완
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDailyMessageStatus(Long userId) {
        String todayKey = DAILY_MESSAGE_COUNT_KEY + userId + ":" + LocalDateTime.now().toLocalDate();
        String countStr = redisTemplate.opsForValue().get(todayKey);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
        
        return Map.of(
            "currentCount", currentCount,
            "maxCount", MAX_DAILY_MESSAGES,
            "remainingCount", Math.max(0, MAX_DAILY_MESSAGES - currentCount)
        );
    }

    @Transactional(readOnly = true)
    public List<Message> getConversation(User user, Long partnerId) {
        User partner = userRepository.findById(partnerId).orElseThrow();
        return messageRepository.findConversationWithPartner(user, partner);
    }

    @Transactional
    public void deleteConversation(User user, Long partnerId) {
        User partner = userRepository.findById(partnerId).orElseThrow();
        // [시니어 조치] 시스템 메시지 필터링 없이 모든 메시지 조회 (대화방 완전 삭제용)
        List<Message> messages = messageRepository.findAllMessagesWithPartnerForDeletion(user, partner);
        log.info("[deleteConversation] user={}, partnerId={}, 조회된 메시지 수={}", user.getId(), partnerId, messages.size());
        for (Message msg : messages) {
            // [시니어 조치] sender가 null이거나 system 메시지인 경우도 처리
            if (msg.getSender() != null && msg.getSender().getId().equals(user.getId())) {
                log.info("[deleteConversation] 메시지 ID={} - deleteBySender 처리 (sender={})", msg.getId(), msg.getSender().getId());
                msg.deleteBySender();
            } else if (msg.getReceiver().getId().equals(user.getId())) {
                log.info("[deleteConversation] 메시지 ID={} - deleteByReceiver 처리 (receiver={})", msg.getId(), msg.getReceiver().getId());
                msg.deleteByReceiver();
            } else {
                log.warn("[deleteConversation] 메시지 ID={} - 처리 실패 (sender={}, receiver={})", msg.getId(),
                    msg.getSender() != null ? msg.getSender().getId() : "null", msg.getReceiver().getId());
            }
        }
    }

    @Transactional
    public void deleteMessage(Long messageId, User user) {
        Message message = messageRepository.findById(messageId).orElseThrow();
        log.info("[deleteMessage] messageId={}, user={}", messageId, user.getId());
        if (message.getSender() != null && message.getSender().getId().equals(user.getId())) {
            log.info("[deleteMessage] messageId={} - deleteBySender 처리", messageId);
            message.deleteBySender();
        } else if (message.getReceiver().getId().equals(user.getId())) {
            log.info("[deleteMessage] messageId={} - deleteByReceiver 처리", messageId);
            message.deleteByReceiver();
        } else {
            log.warn("[deleteMessage] messageId={} - 처리 실패", messageId);
        }
    }

    @Transactional
    public Message editMessage(Long messageId, User user, String content) {
        Message message = messageRepository.findById(messageId).orElseThrow();
        if (!message.getSender().getId().equals(user.getId())) throw new IllegalArgumentException("수정 권한 없음");
        message.updateContent(content);
        return message;
    }

    @Transactional
    public void readMessage(Long messageId, User user) {
        Message message = messageRepository.findById(messageId).orElseThrow();
        if (message.getReceiver().getId().equals(user.getId())) message.markAsRead();
    }

    @Transactional
    public void markConversationAsRead(User user, Long partnerId) {
        User partner = userRepository.findById(partnerId).orElseThrow();
        messageRepository.findUnreadMessagesWithPartner(user, partner).forEach(Message::markAsRead);
    }

    @Transactional
    public void sendAdminResultSystemMessage(User sender, User receiver, String content, Long visibleToUserId, boolean isRoomRestricted, Long relatedTargetId) {
        // [시니어 조치] AI 분석 리포트 섹션 제거 및 호출자가 전달한 content 직접 사용
        // 이전에는 여기서 AI 분석 결과를 바탕으로 메시지를 재조립했으나, 
        // 이제는 AdminBoardService에서 정교하게 생성된 content를 그대로 발송함.
        
        Message msg = Message.builder()
                .sender(sender) // 기존 대화방 유지를 위해 파트너 정보를 sender로 설정
                .receiver(receiver)
                .content(content)
                .isSystem(true)
                .isRead(false)
                .visibleToUserId(visibleToUserId)
                .isRoomRestricted(isRoomRestricted)
                .relatedTargetId(relatedTargetId)
                .build();
        
        messageRepository.save(msg);
    }

    @Transactional
    public void clearSystemMessages(Long relatedTargetId) {
        messageRepository.deleteAll(messageRepository.findAllByIsSystemTrueAndRelatedTargetId(relatedTargetId));
    }

    @Transactional
    public void restoreMessage(Long messageId) {
        messageRepository.findById(messageId).ifPresent(m -> { m.restore(); clearSystemMessages(messageId); });
    }

    private void saveFiles(Message message, List<MultipartFile> files) {
        try {
            List<String> urls = fileStorageService.upload(files, "messages");
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                if (file.isEmpty()) continue;
                message.addAttachment(MessageFile.builder()
                    .message(message)
                    .originalFileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .fileUrl(urls.get(i))
                    .build());
            }
        } catch (IOException e) { log.error("파일 저장 실패", e); }
    }

    @Transactional(readOnly = true)
    public Page<Message> getLatestMessagesGroupedByPartner(User user, Pageable pageable) {
        return messageRepository.findLatestMessagesByPartnersWithTime(user, LocalDateTime.now().minusDays(90), pageable);
    }

    @Transactional(readOnly = true)
    public long countUnreadMessagesWithPartner(User user, User partner) {
        return messageRepository.countUnreadMessagesWithPartner(user, partner);
    }

    @Transactional
    public void blockUser(User blocker, Long blockedId, String reason, boolean isSystemBlock) {
        User blocked = userRepository.findById(blockedId).orElseThrow();
        log.info("[blockUser] blocker={}, blocked={}, isSystemBlock={}", blocker.getId(), blockedId, isSystemBlock);

        // [시니어 조치] 수동 차단시에만 대화방을 숨김 처리하고,
        // 관리자 제재에 의한 시스템 차단 시에는 대화 맥락 보존을 위해 삭제하지 않음.
        // 이미 차단되어 있어도 사용자의 수동 차단 요청이면 대화방 삭제 처리
        if (!isSystemBlock) {
            log.info("[blockUser] 수동 차단 - deleteConversation 호출");
            deleteConversation(blocker, blockedId);
        } else {
            log.info("[blockUser] 시스템 차단 - deleteConversation 호출 안 함 (대화방 유지, 차단만 처리)");
        }

        if (userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            log.info("[blockUser] 이미 차단 레코드 존재 - early return");
            return;
        }
        log.info("[blockUser] 차단 레코드 생성");
        userBlockRepository.save(UserBlock.builder().blocker(blocker).blocked(blocked).reason(reason).isSystemBlock(isSystemBlock).build());
    }

    @Transactional
    public void blockUser(User blocker, Long blockedId) { blockUser(blocker, blockedId, "사용자 수동 차단", false); }

    @Transactional
    public void unblockUser(User blocker, Long blockedId) {
        User blocked = userRepository.findById(blockedId).orElseThrow();
        userBlockRepository.deleteByBlockerAndBlocked(blocker, blocked);
        messageRepository.clearRestrictionBetweenUsers(blocker, blocked);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBlockedUsersWithDetails(User blocker) {
        return userBlockRepository.findAllByBlocker(blocker).stream().map(b -> {
            Map<String, Object> map = new java.util.HashMap<>();
            User blocked = b.getBlocked();
            map.put("id", blocked.getId());
            map.put("publicId", blocked.getPublicId()); // [시니어 조치] 공개 ID 추가
            map.put("nickname", blocked.getNickname() != null ? blocked.getNickname() : blocked.getUsername());
            map.put("reason", b.getReason());
            map.put("isSystemBlock", b.isSystemBlock());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserBlock> getBlockedUsers(User blocker) { return userBlockRepository.findAllByBlocker(blocker); }

    @Transactional
    public void reportMessage(Long messageId, User user) {
        messageRepository.findById(messageId).ifPresent(Message::report);
    }
}
