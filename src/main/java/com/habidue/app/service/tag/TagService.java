package com.habidue.app.service.tag;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.notice.NoticeKeywordMetadata;
import com.habidue.app.domain.notice.NoticeStatus;
import com.habidue.app.domain.tag.NoticeTag;
import com.habidue.app.domain.tag.Tag;
import com.habidue.app.domain.tag.TagType;
import com.habidue.app.dto.tag.TagResponseDto;
import com.habidue.app.repository.notice.NoticeKeywordMetadataRepository;
import com.habidue.app.repository.notice.NoticeTagRepository;
import com.habidue.app.repository.tag.TagRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.habidue.app.service.notice.event.NoticeCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 중앙 태깅 엔진 - DB 메타데이터 및 상태 정보를 기반으로 공고에 태그를 부여합니다.
 */
@Slf4j
@Service
public class TagService {

    private final TagRepository tagRepository;
    private final NoticeTagRepository noticeTagRepository;
    private final NoticeKeywordMetadataRepository keywordMetadataRepository;
    private final com.habidue.app.repository.notice.NoticeRepository noticeRepository;
    private final jakarta.persistence.EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    public TagService(TagRepository tagRepository, 
                      NoticeTagRepository noticeTagRepository, 
                      NoticeKeywordMetadataRepository keywordMetadataRepository, 
                      com.habidue.app.repository.notice.NoticeRepository noticeRepository, 
                      jakarta.persistence.EntityManager entityManager, 
                      ApplicationEventPublisher eventPublisher,
                      org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.tagRepository = tagRepository;
        this.noticeTagRepository = noticeTagRepository;
        this.keywordMetadataRepository = keywordMetadataRepository;
        this.noticeRepository = noticeRepository;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    }

    private List<NoticeKeywordMetadata> cachedKeywords = new ArrayList<>();

    /** "name:TYPE" → Tag 인메모리 캐시 (findByNameAndType DB 쿼리 제거) */
    private final Map<String, Tag> tagCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public void refreshCache() {
        this.cachedKeywords = keywordMetadataRepository.findAll();
        tagCache.clear(); // 기존 메모리 캐시를 비워 삭제된 데이터와의 불일치를 방지합니다.
        tagRepository.findAll().forEach(t -> tagCache.put(cacheKey(t.getName(), t.getType()), t));
        log.info("TagService: {} keywords cached, {} tags cached.", cachedKeywords.size(), tagCache.size());
    }

    private String cacheKey(String name, TagType type) {
        return name + ":" + type.name();
    }

    /**
     * [크롤러 전용] provider 태그 + 자동 분류를 한 번에 처리합니다.
     * notice_tag 기존 항목을 1회 로드 후 인메모리 Set으로 중복 체크 → N+1 제거.
     */
    @Transactional
    public void applyTagsToNotice(Notice notice, List<String> providerTagNames, boolean shouldNotify) {
        // 기존 notice_tag를 1회 쿼리로 로드
        Set<Long> existingTagIds = noticeTagRepository.findAllByNoticeWithTag(notice).stream()
                .map(nt -> nt.getTag().getId())
                .collect(Collectors.toSet());

        // 1. Provider 태그 추가
        if (providerTagNames != null) {
            for (String name : providerTagNames) {
                if (name == null || name.isBlank()) continue;
                Tag tag = getOrCreateTag(name.trim(), TagType.PROVIDER);
                if (existingTagIds.add(tag.getId())) {
                    noticeTagRepository.save(NoticeTag.builder().notice(notice).tag(tag).build());
                }
            }
        }

        // 2. 자동 분류
        String analysisTarget = ((notice.getTitle() != null ? notice.getTitle() : "") + " "
                + (notice.getContent() != null ? notice.getContent() : "")).toLowerCase();
        String maskingTarget = analysisTarget;

        NoticeStatus detectedStatus = NoticeStatus.INFO;

        List<NoticeKeywordMetadata> sortedKeywords = cachedKeywords.stream()
                .sorted((a, b) -> {
                    if (a.getTagType() == TagType.IGNORE && b.getTagType() != TagType.IGNORE) return -1;
                    if (a.getTagType() != TagType.IGNORE && b.getTagType() == TagType.IGNORE) return 1;
                    return b.getKeyword().length() - a.getKeyword().length();
                })
                .collect(Collectors.toList());

        for (NoticeKeywordMetadata metadata : sortedKeywords) {
            String keyword = metadata.getKeyword().toLowerCase();
            if (!maskingTarget.contains(keyword)) continue;

            maskingTarget = maskingTarget.replace(keyword, "#".repeat(keyword.length()));

            if (metadata.getTagType() == TagType.IGNORE) continue;

            if (metadata.getTagType() == TagType.SYSTEM && metadata.getTargetStatus() != null) {
                if (metadata.getTargetStatus().getPriority() >= detectedStatus.getPriority()) {
                    detectedStatus = metadata.getTargetStatus();
                }
            } else {
                Tag tag = getOrCreateTag(metadata.getKeyword(), metadata.getTagType());
                if (existingTagIds.add(tag.getId())) {
                    noticeTagRepository.save(NoticeTag.builder().notice(notice).tag(tag).build());
                }
            }
        }

        // 3. 마감일 기반 상태 보정 (당일 23:59:59까지는 접수 중으로 인정)
        NoticeStatus finalStatus = detectedStatus;
        if (notice.getDeadline() != null) {
            java.time.LocalDateTime endOfDay = notice.getDeadline().withHour(23).withMinute(59).withSecond(59);
            if (endOfDay.isBefore(java.time.LocalDateTime.now())) {
                finalStatus = detectedStatus.getExpired();
            }
        }

        Tag systemTag = getOrCreateTag(finalStatus.getDescription(), TagType.SYSTEM);
        if (existingTagIds.add(systemTag.getId())) {
            noticeTagRepository.save(NoticeTag.builder().notice(notice).tag(systemTag).build());
        }

        noticeRepository.updateStatus(notice.getId(), finalStatus);

        if (shouldNotify) {
            eventPublisher.publishEvent(new NoticeCreatedEvent(notice));
        }
    }

    /**
     * 특정 공고에 수동으로 태그 목록을 추가합니다. (관리자/기타 용도)
     */
    @Transactional
    public void addTagsToNotice(Notice notice, List<String> tagNames, TagType type) {
        if (tagNames == null) return;
        for (String name : tagNames) {
            if (name == null || name.isBlank()) continue;
            Tag tag = getOrCreateTag(name.trim(), type);
            linkNoticeAndTag(notice, tag);
        }
    }

    /**
     * 공고 내용을 분석하여 자동으로 태그를 분류하고 추가합니다. (관리자/기타 용도)
     */
    @Transactional
    public void autoClassifyAndAddTags(Notice notice, boolean shouldNotify) {
        String originalTitle = notice.getTitle() != null ? notice.getTitle() : "";
        String originalContent = notice.getContent() != null ? notice.getContent() : "";
        String analysisTarget = (originalTitle + " " + originalContent).toLowerCase();
        String maskingTarget = analysisTarget;

        NoticeStatus detectedStatus = NoticeStatus.INFO;
        Set<Long> processedTagIds = new HashSet<>();

        List<NoticeKeywordMetadata> sortedKeywords = cachedKeywords.stream()
                .sorted((a, b) -> {
                    if (a.getTagType() == TagType.IGNORE && b.getTagType() != TagType.IGNORE) return -1;
                    if (a.getTagType() != TagType.IGNORE && b.getTagType() == TagType.IGNORE) return 1;
                    return b.getKeyword().length() - a.getKeyword().length();
                })
                .collect(Collectors.toList());

        for (NoticeKeywordMetadata metadata : sortedKeywords) {
            String keyword = metadata.getKeyword().toLowerCase();
            if (maskingTarget.contains(keyword)) {
                maskingTarget = maskingTarget.replace(keyword, "#".repeat(keyword.length()));
                if (metadata.getTagType() == TagType.IGNORE) continue;
                if (metadata.getTagType() == TagType.SYSTEM && metadata.getTargetStatus() != null) {
                    if (metadata.getTargetStatus().getPriority() >= detectedStatus.getPriority()) {
                        detectedStatus = metadata.getTargetStatus();
                    }
                } else {
                    Tag tag = getOrCreateTag(metadata.getKeyword(), metadata.getTagType());
                    if (!processedTagIds.contains(tag.getId())) {
                        linkNoticeAndTag(notice, tag);
                        processedTagIds.add(tag.getId());
                    }
                }
            }
        }

        NoticeStatus finalStatus = detectedStatus;
        if (notice.getDeadline() != null) {
            java.time.LocalDateTime endOfDay = notice.getDeadline().withHour(23).withMinute(59).withSecond(59);
            if (endOfDay.isBefore(java.time.LocalDateTime.now())) {
                finalStatus = detectedStatus.getExpired();
            }
        }

        Tag systemTag = getOrCreateTag(finalStatus.getDescription(), TagType.SYSTEM);
        linkNoticeAndTag(notice, systemTag);
        noticeRepository.updateStatus(notice.getId(), finalStatus);

        if (shouldNotify) {
            eventPublisher.publishEvent(new NoticeCreatedEvent(notice));
        }
    }

    @Transactional
    public void syncSystemTags() {
        log.info("TagService: Syncing system tags from Enum definitions...");
        for (NoticeStatus status : NoticeStatus.values()) {
            getOrCreateTag(status.getDescription(), TagType.SYSTEM);
        }
    }

    private void linkNoticeAndTag(Notice notice, Tag tag) {
        if (noticeTagRepository.findByNoticeAndTag(notice, tag).isEmpty()) {
            noticeTagRepository.save(NoticeTag.builder().notice(notice).tag(tag).build());
        }
    }

    private Tag getOrCreateTag(String name, TagType type) {
        return tagCache.computeIfAbsent(cacheKey(name, type), k ->
                tagRepository.findByNameAndType(name, type).orElseGet(() -> {
                    try {
                        Tag saved = tagRepository.saveAndFlush(Tag.builder().name(name).type(type).build());
                        return saved;
                    } catch (Exception e) {
                        return tagRepository.findByNameAndType(name, type).orElseThrow();
                    }
                })
        );
    }

    // [시니어 조치] 거대 트랜잭션 방지를 위해 클래스 레벨 @Transactional 의존을 피하고 개별 처리
    public void reprocessAllNotices() {
        syncSystemTags();
        refreshCache();
        List<Notice> allNotices = noticeRepository.findAll();
        if (allNotices == null || allNotices.isEmpty()) return;
        tagAllNotices(allNotices);
    }

    /** 진행률 추적용 변수 */
    private final java.util.concurrent.atomic.AtomicInteger totalCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger processedCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicBoolean isProcessing = new java.util.concurrent.atomic.AtomicBoolean(false);

    public boolean isProcessing() { return isProcessing.get(); }
    public int getTotalCount() { return totalCount.get(); }
    public int getProcessedCount() { return processedCount.get(); }

    public void tagAllNotices(List<Notice> notices) {
        int total = notices.size();
        totalCount.set(total);
        processedCount.set(0);
        isProcessing.set(true);

        log.info("================================================================");
        log.info("TagService: 대량 태깅 작업 시작 (총 {}건)", total);
        log.info("================================================================");
        
        try {
            for (int i = 0; i < total; i++) {
                final int index = i;
                try {
                    Notice notice = notices.get(index);
                    // [시니어 조치] TransactionTemplate을 사용하여 개별 공고마다 확실한 커밋 보장
                    transactionTemplate.execute(status -> {
                        autoClassifyAndAddTags(notice, false);
                        return null;
                    });
                    processedCount.incrementAndGet();
                    
                    if ((index + 1) % 100 == 0 || (index + 1) == total) {
                        double percent = ((double) (index + 1) / total) * 100;
                        log.info("TagService: 진행 중... [{} / {}] ({})", 
                            index + 1, total, String.format("%.1f%%", percent)
                        );
                    }
                } catch (Exception e) {
                    log.error("TagService: [에러] 인덱스 {} 처리 중 오류 발생: {}", index, e.getMessage());
                }
            }
        } finally {
            isProcessing.set(false);
        }
        
        log.info("================================================================");
        log.info("TagService: 모든 공고 태깅 완료!");
        log.info("================================================================");
    }

    // --- 관리자 전용 ---

    @Transactional
    public Tag createTag(String name, TagType type) {
        if (tagRepository.findByNameAndType(name, type).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 태그입니다.");
        }
        return tagRepository.save(Tag.builder().name(name).type(type).build());
    }

    @Transactional(readOnly = true)
    public List<TagResponseDto> getAllTags() {
        return tagRepository.findAll().stream().map(TagResponseDto::new).collect(Collectors.toList());
    }

    @Transactional
    public Tag updateTag(Long tagId, String name, TagType type) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new NoSuchElementException("태그를 찾을 수 없습니다. ID: " + tagId));
        tag.update(name, type);
        return tagRepository.save(tag);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getTagUsage(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new NoSuchElementException("태그를 찾을 수 없습니다. ID: " + tagId));
        Map<String, Long> usage = new HashMap<>();
        usage.put("noticeCount", (long) tag.getNoticeTags().size());
        usage.put("userCount", (long) tag.getUserTags().size());
        return usage;
    }

    @Transactional
    public void deleteTag(Long tagId) {
        tagRepository.deleteById(tagId);
    }

    @Transactional(readOnly = true)
    public List<TagResponseDto> searchTags(String name) {
        return tagRepository.findByNameContaining(name).stream().map(TagResponseDto::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TagResponseDto> getPopularTags(int limit) {
        return tagRepository.findTopTags(org.springframework.data.domain.PageRequest.of(0, 20)).stream()
                .filter(tag -> tag.getType() != TagType.IGNORE && tag.getType() != TagType.SYSTEM)
                .limit(limit)
                .map(TagResponseDto::new)
                .collect(Collectors.toList());
    }
}
