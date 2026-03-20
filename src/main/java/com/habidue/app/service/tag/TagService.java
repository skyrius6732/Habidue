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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 중앙 태깅 엔진 - DB 메타데이터 및 상태 정보를 기반으로 공고에 태그를 부여합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final NoticeTagRepository noticeTagRepository;
    private final NoticeKeywordMetadataRepository keywordMetadataRepository;
    private final com.habidue.app.repository.notice.NoticeRepository noticeRepository;
    private final jakarta.persistence.EntityManager entityManager;

    private List<NoticeKeywordMetadata> cachedKeywords = new ArrayList<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public void refreshCache() {
        this.cachedKeywords = keywordMetadataRepository.findAll();
        log.info("TagService: {} keywords cached.", cachedKeywords.size());
    }

    /**
     * 특정 공고에 수동으로 태그 목록을 추가합니다. (예: 수집기에서 호출)
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
     * 공고 내용을 분석하여 자동으로 태그를 분류하고 추가합니다.
     */
    @Transactional
    public void autoClassifyAndAddTags(Notice notice) {
        String originalTitle = notice.getTitle() != null ? notice.getTitle() : "";
        String originalContent = notice.getContent() != null ? notice.getContent() : "";
        String analysisTarget = (originalTitle + " " + originalContent).toLowerCase();
        
        // [시니어 조치] 마스킹 처리를 위한 제목 복사본
        // 원문은 유지하되, 태그 추출 과정에서 특정 영역을 제외하기 위해 사용
        String maskingTarget = analysisTarget;
        
        NoticeStatus detectedStatus = NoticeStatus.INFO;
        Set<Long> processedTagIds = new HashSet<>();

        // [시니어 로직] 키워드 우선순위 정렬 (IGNORE 타입을 가장 먼저 처리)
        List<NoticeKeywordMetadata> sortedKeywords = cachedKeywords.stream()
                .sorted((a, b) -> {
                    // 1순위: IGNORE 타입 우선
                    if (a.getTagType() == TagType.IGNORE && b.getTagType() != TagType.IGNORE) return -1;
                    if (a.getTagType() != TagType.IGNORE && b.getTagType() == TagType.IGNORE) return 1;
                    // 2순위: 긴 단어 우선 (부분 일치 방지)
                    return b.getKeyword().length() - a.getKeyword().length();
                })
                .collect(Collectors.toList());

        for (NoticeKeywordMetadata metadata : sortedKeywords) {
            String keyword = metadata.getKeyword().toLowerCase();
            
            // 마스킹된 텍스트에서 키워드 검색
            if (maskingTarget.contains(keyword)) {
                
                // 1. 공통 마스킹 처리: 매칭된 영역을 제외하여 하위 키워드 오진 방지
                // 예: '지원주택' 매칭 시 '원주'가 추출되지 않도록 해당 영역을 #으로 치환
                maskingTarget = maskingTarget.replace(keyword, "#".repeat(keyword.length()));

                // A. 무시단어(IGNORE) 처리: 마스킹만 하고 태그는 부착하지 않음
                if (metadata.getTagType() == TagType.IGNORE) {
                    log.debug("  > [무시단어 마스킹] '{}' 영역 제외", keyword);
                    continue; 
                }

                // B. 시스템 상태 판별
                if (metadata.getTagType() == TagType.SYSTEM && metadata.getTargetStatus() != null) {
                    if (metadata.getTargetStatus().getPriority() >= detectedStatus.getPriority()) {
                        detectedStatus = metadata.getTargetStatus();
                    }
                } 
                
                // C. 일반 태그 부착 (이미 처리된 태그 중복 방지)
                else {
                    Tag tag = getOrCreateTag(metadata.getKeyword(), metadata.getTagType());
                    if (!processedTagIds.contains(tag.getId())) {
                        linkNoticeAndTag(notice, tag);
                        processedTagIds.add(tag.getId());
                        log.debug("  > [태그 부착 및 마스킹] '{}' ({})", keyword, metadata.getTagType());
                    }
                }
            }
        }

        // 2. [날짜 보정] 마감일 기준 상태 전이 로직 유지
        NoticeStatus finalStatus = detectedStatus;
        if (notice.getDeadline() != null && notice.getDeadline().isBefore(java.time.LocalDateTime.now())) {
            finalStatus = detectedStatus.getExpired();
        }

        // 3. 최종 결정된 시스템 상태 태그 부여
        Tag systemTag = getOrCreateTag(finalStatus.getDescription(), TagType.SYSTEM);
        linkNoticeAndTag(notice, systemTag);
        
        noticeRepository.updateStatus(notice.getId(), finalStatus);
    }

    /**
     * 시스템 상태 기반 태그를 최신화합니다.
     */
    @Transactional
    public void syncSystemTags() {
        log.info("TagService: Syncing system tags from Enum definitions...");
        for (NoticeStatus status : NoticeStatus.values()) {
            getOrCreateTag(status.getDescription(), TagType.SYSTEM);
        }
    }

    /**
     * 공고의 현재 상태를 기반으로 SYSTEM 타입 태그를 부여합니다. (사용 중단 예정)
     */
    private void addSystemStatusTag(Notice notice) {
        // autoClassifyAndAddTags 에서 직접 처리함
    }

    private void linkNoticeAndTag(Notice notice, Tag tag) {
        if (noticeTagRepository.findByNoticeAndTag(notice, tag).isEmpty()) {
            noticeTagRepository.save(NoticeTag.builder()
                    .notice(notice)
                    .tag(tag)
                    .build());
        }
    }

    private Tag getOrCreateTag(String name, TagType type) {
        return tagRepository.findByNameAndType(name, type)
                .orElseGet(() -> {
                    try {
                        return tagRepository.saveAndFlush(Tag.builder().name(name).type(type).build());
                    } catch (Exception e) {
                        return tagRepository.findByNameAndType(name, type).orElseThrow();
                    }
                });
    }

    /**
     * 모든 공고에 대해 태깅을 재실행합니다. (관리자용)
     */
    @Transactional
    public void tagAllNotices(List<Notice> notices) {
        log.info("TagService: Starting bulk tagging for {} notices with Batch processing...", notices.size());
        
        for (int i = 0; i < notices.size(); i++) {
            Notice notice = notices.get(i);
            
            // [최적화] 벌크 삭제 적용 (N+1 삭제 방지)
            noticeTagRepository.deleteByNoticeBulk(notice);
            
            // 재분석 및 태깅
            autoClassifyAndAddTags(notice);
            
            // [전문가 최적화] 50건마다 영속성 컨텍스트 비우기 (메모리 및 속도 관리)
            if (i > 0 && i % 50 == 0) {
                entityManager.flush();
                entityManager.clear();
                log.debug("TagService: Batch processed {} notices, cleared EntityManager.", i);
            }
        }
        log.info("TagService: Bulk tagging completed.");
    }

    /**
     * [신설] DB 내의 모든 공고를 전수 조사하여 상태와 태그를 재설정합니다.
     */
    @Transactional
    public void reprocessAllNotices() {
        syncSystemTags(); // 기본 6단계 태그 확보
        refreshCache(); // 최신 메타데이터 캐시 로드
        
        List<Notice> allNotices = noticeRepository.findAll();
        if (allNotices == null || allNotices.isEmpty()) return;
        tagAllNotices(allNotices);
    }

    // --- [관리자 전용 기능] ---

    @Transactional
    public Tag createTag(String name, TagType type) {
        if (tagRepository.findByNameAndType(name, type).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 태그입니다.");
        }
        return tagRepository.save(Tag.builder().name(name).type(type).build());
    }

    @Transactional(readOnly = true)
    public List<TagResponseDto> getAllTags() {
        return tagRepository.findAll().stream()
                .map(TagResponseDto::new)
                .collect(Collectors.toList());
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
        return tagRepository.findByNameContaining(name).stream()
                .map(TagResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * [시니어 조치] 인기 키워드 조회를 위한 상위 태그 추출 (시스템/무시 타입 제외)
     */
    @Transactional(readOnly = true)
    public List<TagResponseDto> getPopularTags(int limit) {
        return tagRepository.findTopTags(org.springframework.data.domain.PageRequest.of(0, 20)).stream()
                .filter(tag -> tag.getType() != TagType.IGNORE && tag.getType() != TagType.SYSTEM)
                .limit(limit)
                .map(TagResponseDto::new)
                .collect(Collectors.toList());
    }
}
