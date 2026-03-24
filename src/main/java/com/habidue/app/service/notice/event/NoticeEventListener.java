package com.habidue.app.service.notice.event;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.notification.NotificationType;
import com.habidue.app.domain.tag.NoticeTag;
import com.habidue.app.domain.tag.UserTag;
import com.habidue.app.repository.tag.UserTagRepository;
import com.habidue.app.repository.notice.NoticeTagRepository;
import com.habidue.app.domain.usernotice.UserNotice;
import com.habidue.app.repository.notice.UserNoticeRepository;
import com.habidue.app.repository.notification.NotificationRepository;
import com.habidue.app.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 공고 관련 이벤트를 처리하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeEventListener {

    private final UserTagRepository userTagRepository;
    private final NoticeTagRepository noticeTagRepository;
    private final UserNoticeRepository userNoticeRepository;
    private final com.habidue.app.repository.notice.NoticeRepository noticeRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    /**
     * 새 공고 생성 및 태그 재설정 완료 시 알림 발송 (커밋 후에만 동작)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNoticeCreatedEvent(NoticeCreatedEvent event) {
        // [시니어 조치] 넘겨받은 객체 대신 ID로 DB에서 최신 상태 재조회 (REQUIRES_NEW 정합성 보장)
        Notice notice = noticeRepository.findById(event.getNotice().getId())
                .orElse(event.getNotice());
        
        log.info("[Notification] Processing Notice: {}, Status: {}", notice.getId(), notice.getStatus());

        // 1. [관심 공고 상태 변경 알림] 해당 공고를 찜한 유저들에게 알림 발송
        List<UserNotice> scrapers = userNoticeRepository.findAllByNotice(notice);
        log.info("[Notification] Found {} scrapers for notice {}", scrapers.size(), notice.getId());
        
        for (UserNotice un : scrapers) {
            String content = String.format("🔄 찜하신 공고의 상태가 [%s]로 변경되었습니다: %s", 
                    notice.getStatus().getDescription(), notice.getTitle());
            log.info("[Notification] Sending status change notification to user {}", un.getUser().getId());
            notificationService.send(un.getUser(), NotificationType.NOTICE_STATUS_CHANGE, content, notice.getId(), null);
        }

        // 2. [키워드 매칭 알림] 태그 분석을 통한 구독자 알림
        List<NoticeTag> tags = noticeTagRepository.findAllByNoticeWithTag(notice);
        if (tags == null || tags.isEmpty()) return;

        Set<Long> tagIds = tags.stream()
                .map(nt -> nt.getTag().getId())
                .collect(Collectors.toSet());

        List<UserTag> matchedUserTags = userTagRepository.findAllByTagIdIn(tagIds);
        Set<com.habidue.app.domain.user.User> targets = matchedUserTags.stream()
                .map(UserTag::getUser)
                .collect(Collectors.toSet());

        String content = String.format("🔍 설정하신 키워드와 일치하는 새 공고가 등록되었습니다: %s", notice.getTitle());
        for (com.habidue.app.domain.user.User receiver : targets) {
            // [시니어 조치] 중복 발송 방지: 동일 공고에 대해 이미 키워드 알림을 보냈다면 스킵
            if (!notificationRepository.existsByUserIdAndTypeAndRelatedTargetId(receiver.getId(), NotificationType.NOTICE_KEYWORD, notice.getId())) {
                notificationService.send(receiver, NotificationType.NOTICE_KEYWORD, content, notice.getId(), null);
            }
        }
    }
}
