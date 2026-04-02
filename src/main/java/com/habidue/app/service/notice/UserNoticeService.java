package com.habidue.app.service.notice;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserActivityStats;
import com.habidue.app.domain.usernotice.UserNotice;
import com.habidue.app.dto.usernotice.UserNoticeRequestDto;
import com.habidue.app.dto.usernotice.UserNoticeResponseDto;
import com.habidue.app.repository.notice.NoticeRepository;
import com.habidue.app.repository.notice.UserNoticeRepository;
import com.habidue.app.repository.user.UserRepository;
import com.habidue.app.repository.user.UserActivityStatsRepository;
import com.habidue.app.service.badge.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserNoticeService {

    private final UserNoticeRepository userNoticeRepository;
    private final UserRepository userRepository;
    private final NoticeRepository noticeRepository;
    private final UserActivityStatsRepository userActivityStatsRepository;
    private final BadgeService badgeService;
    private final com.habidue.app.service.ranking.RankingService rankingService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("인증된 사용자를 찾을 수 없습니다.");
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new NoSuchElementException("인증된 사용자 정보를 DB에서 찾을 수 없습니다."));
    }

    @Transactional
    public UserNoticeResponseDto addUserNotice(UserNoticeRequestDto userNoticeRequestDto) {
        User currentUser = getCurrentUser();
        // [시니어 조치] Fetch Join을 사용하여 태그까지 한 번에 로드 (성능 최적화 및 에러 방지)
        Notice notice = noticeRepository.findByIdWithTags(userNoticeRequestDto.getNoticeId())
                .orElseThrow(() -> new NoSuchElementException("공고를 찾을 수 없습니다. ID: " + userNoticeRequestDto.getNoticeId()));

        UserNotice saved;
        java.util.Optional<UserNotice> existing = userNoticeRepository.findByUserAndNotice(currentUser, notice);
        
        if (existing.isPresent()) {
            UserNotice un = existing.get();
            if (userNoticeRequestDto.getUserDeadline() != null) un.setUserDeadline(userNoticeRequestDto.getUserDeadline());
            if (userNoticeRequestDto.getMemo() != null) un.setMemo(userNoticeRequestDto.getMemo());
            saved = userNoticeRepository.save(un);
        } else {
            long updatedCount = noticeRepository.incrementInterestCount(notice.getId(), 10);
            
            // [시니어 조치] 최초 10명 달성 시 소통방 해금 보너스 부여 (+100점)
            // notice.getInterestCount() 대신 Atomic 업데이트 결과인 updatedCount 사용
            if (updatedCount >= 10 && Boolean.FALSE.equals(notice.getIsBoardActive())) {
                notice.setIsBoardActive(true);
                rankingService.increaseNoticeScore(notice.getId(), com.habidue.app.service.ranking.RankingService.SCORE_BOARD_UNLOCK);
            }

            saved = userNoticeRepository.save(UserNotice.builder()
                    .user(currentUser).notice(notice)
                    .memo(userNoticeRequestDto.getMemo())
                    .userDeadline(userNoticeRequestDto.getUserDeadline())
                    .build());

            userActivityStatsRepository.insertIgnore(currentUser.getId());
            userActivityStatsRepository.incrementNoticeInterestCount(currentUser.getId());

            UserActivityStats freshStats = userActivityStatsRepository.findById(currentUser.getId()).orElseThrow();
            badgeService.checkAndAwardBadges(freshStats);
            
            // [시니어 조치] 실시간 급상승 랭킹 점수 반영
            rankingService.increaseNoticeScore(notice.getId(), com.habidue.app.service.ranking.RankingService.SCORE_INTEREST);
        }

        // [시니어 조치] User 엔티티는 여전히 지연 로딩 상태일 수 있으므로 강제 초기화 (OSIV가 켜져 있어도 안전장치로 유지)
        org.hibernate.Hibernate.initialize(saved.getUser());

        return new UserNoticeResponseDto(saved);
    }

    public Page<UserNoticeResponseDto> getMyUserNotices(Pageable pageable) {
        User currentUser = getCurrentUser();
        return userNoticeRepository.findByUserId(currentUser.getId(), pageable)
                .map(UserNoticeResponseDto::new);
    }

    @Transactional
    public UserNoticeResponseDto updateUserInfo(Long userNoticeId, String memo, String urls, java.time.LocalDateTime userDeadline) {
        User currentUser = getCurrentUser();
        UserNotice userNotice = userNoticeRepository.findById(userNoticeId)
                .orElseThrow(() -> new NoSuchElementException("관심 공고를 찾을 수 없습니다."));

        if (!userNotice.getUser().getId().equals(currentUser.getId())) throw new IllegalArgumentException("수정 권한이 없습니다.");
        
        userNotice.setMemo(memo);
        userNotice.setReferenceUrls(urls);
        userNotice.setUserDeadline(userDeadline);
        UserNotice saved = userNoticeRepository.save(userNotice);
        return new UserNoticeResponseDto(saved);
    }

    @Transactional
    public void deleteUserNotice(Long userNoticeId) {
        User currentUser = getCurrentUser();
        UserNotice userNoticeToDelete = userNoticeRepository.findById(userNoticeId)
                .orElseThrow(() -> new NoSuchElementException("관심 공고를 찾을 수 없습니다."));

        if (!userNoticeToDelete.getUser().getId().equals(currentUser.getId())) throw new IllegalArgumentException("삭제 권한이 없습니다.");
        
        noticeRepository.decrementInterestCount(userNoticeToDelete.getNotice().getId());
        userNoticeRepository.delete(userNoticeToDelete);
        
        // [시니어 조치] 실시간 급상승 랭킹 점수 차감
        rankingService.increaseNoticeScore(userNoticeToDelete.getNotice().getId(), -com.habidue.app.service.ranking.RankingService.SCORE_INTEREST);

        userActivityStatsRepository.decrementNoticeInterestCount(currentUser.getId());
    }

    @Transactional
    public void deleteUserNoticeByNoticeId(Long noticeId) {
        User currentUser = getCurrentUser();
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공고를 찾을 수 없습니다."));
        
        UserNotice userNotice = userNoticeRepository.findByUserAndNotice(currentUser, notice)
                .orElseThrow(() -> new NoSuchElementException("관심 등록 정보를 찾을 수 없습니다."));
        
        noticeRepository.decrementInterestCount(noticeId);
        userNoticeRepository.delete(userNotice);
        
        // [시니어 조치] 실시간 급상승 랭킹 점수 차감
        rankingService.increaseNoticeScore(noticeId, -com.habidue.app.service.ranking.RankingService.SCORE_INTEREST);

        userActivityStatsRepository.decrementNoticeInterestCount(currentUser.getId());
    }
}
