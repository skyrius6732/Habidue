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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Value("${file.upload-dir:/home/skyrius/habiDue/uploads/messages}")
    private String uploadDir;

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
        List<Message> messages = messageRepository.findConversationWithPartner(user, partner);
        for (Message msg : messages) {
            if (msg.getSender().getId().equals(user.getId())) msg.deleteBySender();
            else if (msg.getReceiver().getId().equals(user.getId())) msg.deleteByReceiver();
        }
    }

    @Transactional
    public void deleteMessage(Long messageId, User user) {
        Message message = messageRepository.findById(messageId).orElseThrow();
        if (message.getSender().getId().equals(user.getId())) message.deleteBySender();
        else if (message.getReceiver().getId().equals(user.getId())) message.deleteByReceiver();
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
        StringBuilder finalContentBuilder = new StringBuilder();
        
        // [시니어 조치] 원본 메시지를 통해 신고자/피신고자 판별 및 위반 정보 추출
        Message origin = relatedTargetId != null ? messageRepository.findById(relatedTargetId).orElse(null) : null;
        boolean hasAiAnalysis = origin != null && origin.getAiAnalysis() != null && !origin.getAiAnalysis().trim().isEmpty();

        if (hasAiAnalysis && origin != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(origin.getAiAnalysis());
                
                String violation = node.path("violationPoint").asText("운영원칙 위반");
                String summary = node.path("summary").asText("");
                double score = node.path("score").asDouble() * 100;

                // 수신자가 피신고자(원본 메시지 발신자)인지 확인
                boolean isReportee = receiver.getId().equals(origin.getSender().getId());

                if (isReportee) {
                    // 피신고자용 템플릿
                    if (isRoomRestricted) {
                        finalContentBuilder.append("🚫 [주의] ").append(receiver.getNickname()).append("님, 회원님께서 사용하신 표현은 '").append(violation).append("' 심각한 운영원칙 위반이 확인되어 본 대화방에서의 메시지 발송이 영구적으로 제한됩니다.");
                    } else {
                        finalContentBuilder.append("⚠️ [경고] ").append(receiver.getNickname()).append("님, 회원님께서 사용하신 표현은 '").append(violation).append("' 정황이 확인되어 제재가 적용되었으며, 본 대화방에서의 메시지 발송이 제한됩니다.");
                    }
                } else {
                    // 신고자용 템플릿
                    if (isRoomRestricted) {
                        finalContentBuilder.append("📢 [안내] 신고하신 대화방에 대해 '").append(violation).append("' 심각한 위반이 확인되어 영구 제한 및 상대방 차단 조치가 완료되었습니다.");
                    } else {
                        finalContentBuilder.append("📢 [안내] 신고하신 내용에 대해 관리자 검토가 완료되었습니다. 해당 대화방은 '").append(violation).append("'로 인해 발송 제한 조치가 취해졌습니다.");
                    }
                }

                // AI 리포트 결합 (하단 구분선 포함)
                finalContentBuilder.append("\n\n------------------------------\n");
                finalContentBuilder.append("🔍 AI 정밀 분석 리포트\n");
                finalContentBuilder.append("● 위험 지수: ").append(String.format("%.0f", score)).append("%\n");
                finalContentBuilder.append("● 위반 항목: ").append(violation).append("\n");
                if (!summary.isEmpty()) {
                    finalContentBuilder.append("● 상세 분석: ").append(summary).append("\n");
                }
                finalContentBuilder.append("------------------------------");

            } catch (Exception e) {
                // 파싱 오류 시 전달받은 기본 메시지 사용
                finalContentBuilder.append(content);
            }
        } else {
            // AI 분석 결과가 없는 경우: 관리자가 입력한 기본 시스템 메시지 발송
            finalContentBuilder.append(content);
        }

        Message msg = Message.builder()
                .sender(sender) // 기존 대화방 유지를 위해 파트너 정보를 sender로 설정
                .receiver(receiver)
                .content(finalContentBuilder.toString())
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
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
        for (MultipartFile file : files) {
            String savedName = UUID.randomUUID().toString() + file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            try {
                Files.copy(file.getInputStream(), Paths.get(uploadDir, savedName));
                message.addAttachment(MessageFile.builder().message(message).originalFileName(file.getOriginalFilename()).fileType(file.getContentType()).fileSize(file.getSize()).fileUrl("/uploads/messages/" + savedName).build());
            } catch (IOException e) { log.error("파일 저장 실패", e); }
        }
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
        if (userBlockRepository.existsByBlockerAndBlocked(blocker, blocked)) return;
        
        // [시니어 조치] 차단 시 해당 유저와의 기존 대화방 내역을 모두 숨김 처리
        deleteConversation(blocker, blockedId);
        
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
