package com.habidue.app.controller;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.user.User;
import com.habidue.app.dto.ApiResponse;
import com.habidue.app.dto.notice.NoticeRequestDto;
import com.habidue.app.dto.notice.NoticeResponseDto;
import com.habidue.app.repository.notice.NoticeTagRepository;
import com.habidue.app.repository.notice.UserNoticeRepository;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.service.notice.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final UserNoticeRepository userNoticeRepository;
    private final NoticeTagRepository noticeTagRepository;
    private final UserRepository userRepository;
    private final com.habidue.app.repository.tag.UserTagRepository userTagRepository; // UserTagRepository로 교체

    @GetMapping
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Page<NoticeResponseDto>>> getNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> sources, // 다중 기관
            @RequestParam(required = false) List<String> statuses, // 다중 상태
            @RequestParam(defaultValue = "alarm") String sortOrder,
            @RequestParam(defaultValue = "false") boolean showOnlyFuture,
            @RequestParam(required = false) Boolean isBoardActive,
            @RequestParam(required = false) Boolean isNew) { // 추가

        Sort sorting = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sorting);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName()).orElse(null);
        List<String> userKeywordsList = null;
        if (currentUser != null) {
            // [중요 수정] Fetch Join 메서드를 사용하여 Tag 엔티티 로딩 보장 (OSIV OFF 대응)
            userKeywordsList = userTagRepository.findAllByUserWithTag(currentUser).stream()
                    .map(ut -> ut.getTag().getName())
                    .collect(Collectors.toList());
        }
        
        // [핵심 수정] 프론트엔드에서 보낸 쉼표 구분 문자열을 확실하게 리스트로 분리
        List<String> processedSources = preprocessFilterList(sources);
        List<String> processedStatuses = preprocessFilterList(statuses);

        // QueryDSL 다중 필터 검색 호출
        log.info("공고 검색 요청: sortOrder={}, isBoardActive={}, isNew={}, sources={}, statuses={}", sortOrder, isBoardActive, isNew, processedSources, processedStatuses);
        Page<Notice> noticePage = noticeService.searchNotices(keyword, processedSources, processedStatuses, sortOrder, userKeywordsList, currentUser, showOnlyFuture, isBoardActive, isNew, pageable);

        // 1. 공고 ID 목록 추출 (즐겨찾기 여부 확인용)
        List<Long> noticeIds = noticePage.getContent().stream()
                .map(Notice::getId)
                .collect(Collectors.toList());

        // 2. [벌크 조회] 현재 사용자가 좋아요를 누른 공고 ID Set (이 로직은 유지 필요)
        Set<Long> favoriteIds = (currentUser != null) 
                ? userNoticeRepository.findFavoriteIds(currentUser, noticeIds)
                : Collections.emptySet();

        // 3. [최적화] 엔티티 필드 직접 사용 (별도 카운팅 쿼리 제거)
        final List<String> finalUserKeywords = userKeywordsList;
        Page<NoticeResponseDto> dtoPage = noticePage.map(notice -> {
            List<com.habidue.app.domain.tag.NoticeTag> latestTags = noticeTagRepository.findAllByNoticeWithTag(notice);
            NoticeResponseDto dto = new NoticeResponseDto(notice, latestTags, finalUserKeywords);
            dto.setFavorite(favoriteIds.contains(notice.getId()));
            return dto;
        });
        
        return ApiResponse.success(dtoPage);
    }

    @GetMapping("/{id}")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<NoticeResponseDto>> getNotice(@PathVariable Long id) {
        Notice notice = noticeService.getNotice(id);
        List<com.habidue.app.domain.tag.NoticeTag> latestTags = noticeTagRepository.findAllByNoticeWithTag(notice);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName()).orElse(null);
        List<String> userKeywordsList = null;
        if (currentUser != null) {
            // [중요 수정] 상세 조회 시에도 Fetch Join 메서드 사용하여 정합성 확보
            userKeywordsList = userTagRepository.findAllByUserWithTag(currentUser).stream()
                    .map(ut -> ut.getTag().getName())
                    .collect(Collectors.toList());
        }

        // [최적화] 상세 조회 시에도 엔티티 필드 사용
        NoticeResponseDto dto = new NoticeResponseDto(notice, latestTags, userKeywordsList);
        
        if (currentUser != null) {
            dto.setFavorite(userNoticeRepository.existsByUserAndNotice(currentUser, notice));
        }
        return ApiResponse.success(dto);
    }

    @PostMapping
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<NoticeResponseDto>> createNotice(@Valid @RequestBody NoticeRequestDto noticeRequestDto) {
        Notice newNotice = noticeService.createNotice(noticeRequestDto);
        return ApiResponse.success(HttpStatus.CREATED, new NoticeResponseDto(newNotice, newNotice.getNoticeTags(), null));
    }

    @PutMapping("/{id}")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<NoticeResponseDto>> updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeRequestDto noticeRequestDto) {
        Notice updatedNotice = noticeService.updateNotice(id, noticeRequestDto);
        return ApiResponse.success(new NoticeResponseDto(updatedNotice, updatedNotice.getNoticeTags(), null));
    }

    @DeleteMapping("/{id}")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ApiResponse.success(HttpStatus.NO_CONTENT, null);
    }

    /**
     * [시니어 조치] 소통방 깨우기 토글
     */
    @PostMapping("/{id}/wakeup")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<Void>> toggleWakeUp(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        
        noticeService.toggleWakeUp(id, currentUser.getId());
        return ApiResponse.success(null);
    }

    /**
     * [시니어 조치] 소통방 깨우기 상태 조회
     */
    @GetMapping("/{id}/wakeup-status")
    @Secured("ROLE_USER")
    public ResponseEntity<ApiResponse<com.habidue.app.dto.notice.NoticeWakeUpStatusDto>> getWakeUpStatus(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName()).orElse(null);
        
        return ApiResponse.success(noticeService.getWakeUpStatus(id, currentUser));
    }

    /**
     * 프론트엔드에서 넘어온 다중 필터 값(쉼표 포함 가능)을 평탄화된 리스트로 변환
     */
    private List<String> preprocessFilterList(List<String> rawList) {
        if (rawList == null || rawList.isEmpty()) return null;
        List<String> processed = rawList.stream()
                .flatMap(s -> java.util.Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !"ALL".equalsIgnoreCase(s))
                .distinct()
                .collect(Collectors.toList());
        return processed.isEmpty() ? null : processed;
    }
}
