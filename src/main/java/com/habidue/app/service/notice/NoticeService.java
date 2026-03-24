package com.habidue.app.service.notice;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.dto.notice.NoticeRequestDto;
import com.habidue.app.repository.notice.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import com.habidue.app.domain.user.User;
import com.habidue.app.domain.tag.Tag;
import com.habidue.app.domain.tag.TagType;
import com.habidue.app.domain.tag.NoticeTag;
import com.habidue.app.repository.tag.TagRepository;
import java.util.stream.Collectors;

import com.habidue.app.service.notice.event.NoticeCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final TagRepository tagRepository;
    private final com.habidue.app.repository.notice.NoticeWakeUpRepository noticeWakeUpRepository;
    private final com.habidue.app.repository.user.UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final com.habidue.app.service.tag.TagService tagService;

    /**
     * [시니어 조치] 매일 새벽 3시, 7일간 활동이 없는 소통방을 휴면(읽기전용) 상태로 전환
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deactivateDormantBoards() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // [운영 정책] 7일간 활동(게시글 작성)이 없으면 휴면 대상으로 간주
        java.time.LocalDateTime dormantThreshold = now.minusDays(7);
        // 최근 7일 이내에 깨어난 기록이 있는 공고는 보호함
        java.time.LocalDateTime revivedThreshold = now.minusDays(7);
        
        List<Notice> dormantNotices = noticeRepository.findDormantCandidates(revivedThreshold, dormantThreshold);
        
        log.info("[KARMA] Running daily dormancy check. Candidates found: {}", dormantNotices.size());
        
        for (Notice notice : dormantNotices) {
            // revivedAt을 초기화하여 다시 깨우기(쓰기 권한 획득)를 유도함
            notice.setRevivedAt(null);
            log.info("[KARMA] Notice Board {} marked as Dormant due to 7 days of inactivity.", notice.getId());
        }
        noticeRepository.saveAll(dormantNotices);
    }

    // QueryDSL 통합 검색 및 정렬
    public Page<Notice> searchNotices(String keyword, List<String> sources, List<String> statuses, String sortOrder, List<String> userKeywords, User currentUser, boolean showOnlyFuture, Boolean isBoardActive, Boolean isNew, Pageable pageable) {
        return noticeRepository.searchNotices(keyword, sources, statuses, sortOrder, userKeywords, currentUser, showOnlyFuture, isBoardActive, isNew, pageable);
    }

    @Transactional
    public Notice createNotice(NoticeRequestDto noticeRequestDto) {
        noticeRepository.findByLink(noticeRequestDto.getLink()).ifPresent(notice -> {
            throw new IllegalArgumentException("이미 존재하는 공고 링크입니다: " + noticeRequestDto.getLink());
        });

        // toEntity() 메서드가 위에서 수정되었으므로 모든 날짜 필드가 포함됨
        Notice notice = noticeRequestDto.toEntity();
        Notice savedNotice = noticeRepository.save(notice);
        
        // [시니어 조치] 자동 태그 분류 (TagService 내부에서 알림 이벤트를 발행함)
        tagService.autoClassifyAndAddTags(savedNotice, true);
        
        return savedNotice;
    }

    public Notice getNotice(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("공고를 찾을 수 없습니다. ID: " + id));
    }

    public Page<Notice> getNotices(Pageable pageable) {
        return noticeRepository.findAll(pageable);
    }

    @Transactional
    public Notice updateNotice(Long id, NoticeRequestDto noticeRequestDto) {
        Notice existingNotice = noticeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("공고를 찾을 수 없습니다. ID: " + id));
        
        existingNotice.setTitle(noticeRequestDto.getTitle());
        existingNotice.setContent(noticeRequestDto.getContent());
        existingNotice.setAnnouncementDate(noticeRequestDto.getAnnouncementDate()); // 추가
        existingNotice.setDeadline(noticeRequestDto.getDeadline());
        existingNotice.setResultDate(noticeRequestDto.getResultDate()); // 추가
        existingNotice.setLink(noticeRequestDto.getLink());
        existingNotice.setSource(noticeRequestDto.getSource());

        if (noticeRequestDto.getStatus() != null) {
            existingNotice.setStatus(noticeRequestDto.getStatus());
        }

        // [시니어 조치] 수정 시에도 태그 재분류 및 알림 발송 (TagService 내부에서 이벤트 발행)
        tagService.autoClassifyAndAddTags(existingNotice, true);

        return noticeRepository.save(existingNotice);
    }

    /**
     * 공고의 현재 상태(status)를 기반으로 시스템 태그를 동기화함
     */
    private void synchronizeSystemTag(Notice notice) {
        String targetTagName = notice.getStatus().getDescription();
        
        // 1. 기존 시스템 태그(상태 관련) 제거
        List<String> statusDescriptions = java.util.Arrays.stream(com.habidue.app.domain.notice.NoticeStatus.values())
                .map(com.habidue.app.domain.notice.NoticeStatus::getDescription)
                .collect(Collectors.toList());

        notice.getNoticeTags().removeIf(nt -> 
            nt.getTag().getType() == TagType.SYSTEM && statusDescriptions.contains(nt.getTag().getName())
        );

        // 2. 새로운 상태 태그 추가
        Tag targetTag = tagRepository.findByNameAndType(targetTagName, TagType.SYSTEM)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(targetTagName).type(TagType.SYSTEM).build()));

        notice.getNoticeTags().add(NoticeTag.builder()
                .notice(notice)
                .tag(targetTag)
                .build());
    }

    @Transactional
    public void deleteNotice(Long id) {
        if (!noticeRepository.existsById(id)) {
            throw new NoSuchElementException("공고를 찾을 수 없습니다. ID: " + id);
        }
        noticeRepository.deleteById(id);
    }

    @Transactional
    public void deleteNoticesInBulk(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        // JPA의 deleteAllById를 사용하여 연관된 데이터(Cascade)까지 안전하게 처리
        noticeRepository.deleteAllById(ids);
    }

    @Transactional
    public Notice save(Notice notice) {
        return noticeRepository.save(notice);
    }

    /**
     * [시니어 조치] 소통방 깨우기 토글 로직
     */
    @Transactional
    public void toggleWakeUp(Long noticeId, Long userId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공고를 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        java.util.Optional<com.habidue.app.domain.notice.NoticeWakeUp> existing = 
                noticeWakeUpRepository.findByNoticeAndUser(notice, user);

        if (existing.isPresent()) {
            noticeWakeUpRepository.delete(existing.get());
        } else {
            noticeWakeUpRepository.save(com.habidue.app.domain.notice.NoticeWakeUp.builder()
                    .notice(notice)
                    .user(user)
                    .build());
        }

        // 상태 체크 및 강제 활성화 여부 판단
        checkAndReviveNotice(notice);
    }

    private void checkAndReviveNotice(Notice notice) {
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(3);
        long currentWakeUps = noticeWakeUpRepository.countByNoticeIdAndCreatedAtAfter(notice.getId(), since);
        
        // [운영 정책] 목표: 관심 유저의 10% 이상, 단 최소 5명 보장
        long targetCount = Math.max(5, (long) (notice.getInterestCount() * 0.1));

        if (currentWakeUps >= targetCount) {
            notice.setRevivedAt(java.time.LocalDateTime.now());
            notice.setIsBoardActive(true); // [시니어 조치] 깨어난 소통방은 게시판도 즉시 활성화
            // lastPostAt이 없으면 현재 시각으로 초기화하여 즉시 다시 닫히지 않게 함
            if (notice.getLastPostAt() == null) {
                notice.setLastPostAt(java.time.LocalDateTime.now());
            }
            noticeRepository.save(notice);

            // [시니어 조치] 깨우기 성공 후 관련 기록 초기화 (다음 휴면 시 다시 0부터 시작하도록)
            noticeWakeUpRepository.deleteAllByNoticeId(notice.getId());
            log.info("[KARMA] Notice Board {} revived and wakeup history cleared.", notice.getId());
        }
    }

    /**
     * [시니어 조치] 현재 깨우기 진행 상태 조회
     */
    public com.habidue.app.dto.notice.NoticeWakeUpStatusDto getWakeUpStatus(Long noticeId, User currentUser) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공고를 찾을 수 없습니다."));

        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(3);
        long currentWakeUps = noticeWakeUpRepository.countByNoticeIdAndCreatedAtAfter(noticeId, since);
        
        // [운영 정책] 목표: 관심 유저의 10% 이상, 단 최소 5명 보장
        long targetCount = Math.max(5, (long) (notice.getInterestCount() * 0.1));
        
        boolean isClicked = false;
        if (currentUser != null) {
            isClicked = noticeWakeUpRepository.existsByNoticeAndUser(notice, currentUser);
        }

        // 현재 활성화 상태인지 판단 (QueryDSL 로직과 동기화)
        boolean isRevived = false;
        if (notice.getRevivedAt() != null) {
            java.time.LocalDateTime lastActivity = notice.getRevivedAt();
            if (notice.getLastPostAt() != null && notice.getLastPostAt().isAfter(lastActivity)) {
                lastActivity = notice.getLastPostAt();
            }
            // 마지막 활동으로부터 7일 이내면 활성 유지
            isRevived = lastActivity.isAfter(java.time.LocalDateTime.now().minusDays(7));
        }

        return com.habidue.app.dto.notice.NoticeWakeUpStatusDto.builder()
                .currentCount(currentWakeUps)
                .targetCount(targetCount)
                .isClicked(isClicked)
                .isRevived(isRevived)
                .build();
    }
}
