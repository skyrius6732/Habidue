package com.habidue.app.service.notice;

import com.habidue.app.config.oauth.UserPrincipal;
import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.user.UserActivityStats;
import com.habidue.app.domain.usernotice.UserNotice;
import com.habidue.app.dto.usernotice.UserNoticeRequestDto;
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
    public UserNotice addUserNotice(UserNoticeRequestDto userNoticeRequestDto) {
        User currentUser = getCurrentUser();
        Notice notice = noticeRepository.findById(userNoticeRequestDto.getNoticeId())
                .orElseThrow(() -> new NoSuchElementException("공고를 찾을 수 없습니다. ID: " + userNoticeRequestDto.getNoticeId()));

        // 이미 존재하는 경우 업데이트로 전환 (카운트 증가 X)
        return userNoticeRepository.findByUserAndNotice(currentUser, notice)
                .map(existing -> {
                    if (userNoticeRequestDto.getUserDeadline() != null) {
                        existing.setUserDeadline(userNoticeRequestDto.getUserDeadline());
                    }
                    if (userNoticeRequestDto.getMemo() != null) {
                        existing.setMemo(userNoticeRequestDto.getMemo());
                    }
                    return userNoticeRepository.save(existing);
                })
                .orElseGet(() -> {
                    // 신규 등록 시에만 카운트 증가 (임계치: 10)
                    noticeRepository.incrementInterestCount(notice.getId(), 10);
                    
                    // [중요] DB 업데이트 후 1차 캐시(영속성 컨텍스트) 내의 notice 객체는 stale 상태임.
                    // 수동으로 상태를 반영하거나 다시 로드하여 데이터 정합성 확보.
                    notice.setInterestCount(notice.getInterestCount() + 1);
                    if (notice.getInterestCount() >= 10) {
                        notice.setIsBoardActive(true);
                    }
                    
                    UserNotice userNotice = UserNotice.builder()
                            .user(currentUser)
                            .notice(notice)
                            .memo(userNoticeRequestDto.getMemo())
                            .userDeadline(userNoticeRequestDto.getUserDeadline())
                            .build();
                            
                    // [시니어 조치] 유저 통계 업데이트 (지연 초기화 적용 - 영속화된 currentUser 사용)
                    UserActivityStats stats = userActivityStatsRepository.findById(currentUser.getId())
                            .orElseGet(() -> userActivityStatsRepository.save(UserActivityStats.createEmpty(currentUser)));
                    
                    stats.incrementNoticeInterestCount();
                    userActivityStatsRepository.save(stats);
                    badgeService.checkAndAwardBadges(stats);

                    return userNoticeRepository.save(userNotice);
                });
    }

    public Page<UserNotice> getMyUserNotices(Pageable pageable) {
        User currentUser = getCurrentUser();
        return userNoticeRepository.findByUserId(currentUser.getId(), pageable);
    }

    // 메모 및 참고 URL 통합 업데이트 (카운트 영향 X)
    @Transactional
    public UserNotice updateUserInfo(Long userNoticeId, String memo, String urls, java.time.LocalDateTime userDeadline) {
        User currentUser = getCurrentUser();
        UserNotice userNotice = userNoticeRepository.findById(userNoticeId)
                .orElseThrow(() -> new NoSuchElementException("관심 공고를 찾을 수 없습니다."));

        if (!userNotice.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        
        userNotice.setMemo(memo);
        userNotice.setReferenceUrls(urls);
        userNotice.setUserDeadline(userDeadline);
        return userNoticeRepository.save(userNotice);
    }

    @Transactional
    public void deleteUserNotice(Long userNoticeId) {
        User currentUser = getCurrentUser();
        UserNotice userNoticeToDelete = userNoticeRepository.findById(userNoticeId)
                .orElseThrow(() -> new NoSuchElementException("관심 공고를 찾을 수 없습니다."));

        if (!userNoticeToDelete.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        
        // 삭제 전 카운트 감소
        noticeRepository.decrementInterestCount(userNoticeToDelete.getNotice().getId());
        userNoticeRepository.delete(userNoticeToDelete);

        // [시니어 조치] 유저 통계 업데이트 (관심 공고 해제)
        userActivityStatsRepository.findById(currentUser.getId()).ifPresent(stats -> {
            stats.decrementNoticeInterestCount();
            userActivityStatsRepository.save(stats);
        });
    }

    @Transactional
    public void deleteUserNoticeByNoticeId(Long noticeId) {
        User currentUser = getCurrentUser();
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoSuchElementException("공고를 찾을 수 없습니다."));
        
        UserNotice userNotice = userNoticeRepository.findByUserAndNotice(currentUser, notice)
                .orElseThrow(() -> new NoSuchElementException("관심 등록 정보를 찾을 수 없습니다."));
        
        // 삭제 전 카운트 감소
        noticeRepository.decrementInterestCount(noticeId);
        userNoticeRepository.delete(userNotice);

        // [시니어 조치] 유저 통계 업데이트 (관심 공고 해제)
        userActivityStatsRepository.findById(currentUser.getId()).ifPresent(stats -> {
            stats.decrementNoticeInterestCount();
            userActivityStatsRepository.save(stats);
        });
    }
}
