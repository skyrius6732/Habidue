package com.habidue.app.service.mail;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.user.User;
import com.habidue.app.repository.notice.NoticeRepository;
import com.habidue.app.repository.notice.UserNoticeRepository;
import com.habidue.app.repository.tag.UserTagRepository;
import com.habidue.app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserRepository userRepository;
    private final NoticeRepository noticeRepository;
    private final UserNoticeRepository userNoticeRepository;
    private final UserTagRepository userTagRepository;
    private final MailService mailService;

    @Transactional(readOnly = true)
    public void generateAndSendWeeklyReports() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.isEmailReportEnabled()) {
                sendUserReport(user);
            }
        }
    }

    @Transactional(readOnly = true)
    public void sendUserReport(User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next3Days = now.plusDays(3);
        LocalDateTime lastWeek = now.minusDays(7);

        // 1. 마감 임박 공고 (관심 등록한 것 중 D-3 이내) - Notice를 미리 Fetch Join하여 1+N 방지
        List<Map<String, String>> urgentNotices = userNoticeRepository.findAllByUserWithNotice(user).stream()
                .map(un -> un.getNotice())
                .filter(n -> n.getDeadline() != null && n.getDeadline().isAfter(now) && n.getDeadline().isBefore(next3Days))
                .map(this::mapToMailDto)
                .collect(Collectors.toList());

        // 2. 맞춤 신규 공고 (사용자 키워드 매칭 + 지난 1주일 이내 게시)
        List<String> userKeywords = userTagRepository.findAllByUser(user).stream()
                .map(ut -> ut.getTag().getName())
                .collect(Collectors.toList());

        List<Map<String, String>> newNotices = List.of();
        if (!userKeywords.isEmpty()) {
            // [참고] 여기서는 간단히 제목 매칭으로 시뮬레이션 (실제론 QueryDSL 검색 서비스 활용 권장)
            newNotices = noticeRepository.findAll().stream()
                    .filter(n -> n.getAnnouncementDate() != null && n.getAnnouncementDate().isAfter(lastWeek))
                    .filter(n -> userKeywords.stream().anyMatch(kw -> n.getTitle().contains(kw)))
                    .limit(5)
                    .map(this::mapToMailDto)
                    .collect(Collectors.toList());
        }

        if (urgentNotices.isEmpty() && newNotices.isEmpty()) {
            log.info("리포트 제외 (보낼 내용 없음): {}", user.getEmail());
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("urgentNotices", urgentNotices);
        variables.put("newNotices", newNotices);

        mailService.sendHtmlMail(user.getReportEmail(), "[HabiDue] 🏠 " + user.getUsername() + "님을 위한 이번 주 맞춤 주거 리포트", "report", variables);
    }

    private Map<String, String> mapToMailDto(Notice n) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        Map<String, String> map = new HashMap<>();
        map.put("title", n.getTitle());
        
        String sourceLabel = n.getSource();
        if ("PRIVATE".equalsIgnoreCase(sourceLabel)) sourceLabel = "민간임대";
        map.put("source", sourceLabel);
        
        map.put("link", n.getLink());
        map.put("deadline", n.getDeadline() != null ? n.getDeadline().format(formatter) : "홈페이지 확인");
        map.put("announcementDate", n.getAnnouncementDate() != null ? n.getAnnouncementDate().format(formatter) : "-");
        return map;
    }
}
