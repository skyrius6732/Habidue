package com.habidue.app.service.notice.collector.lh;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.tag.TagType;
import com.habidue.app.repository.notice.NoticeRepository;
import com.habidue.app.service.tag.TagService;
import com.habidue.app.service.notice.event.NoticeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 서울주거포털 기반 LH 공고 크롤러 (사이트 제공 마감일 활용 버전)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LhNoticeCollectorService {

    private final NoticeRepository noticeRepository;
    private final TagService tagService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String LH_PORTAL_BASE_URL = "https://housing.seoul.go.kr/site/main/lh/publicLease/list";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";

    @Transactional
    public void collectAndSaveLhNotices() {
        log.info("서울주거포털 LH 공고 수집 시작 (자동 마감일 적용)...");
        try {
            Document firstPageDoc = Jsoup.connect(LH_PORTAL_BASE_URL).userAgent(USER_AGENT).timeout(15000).get();
            int totalPages = extractTotalPages(firstPageDoc);

            for (int cp = 1; cp <= totalPages; cp++) {
                String pageUrl = LH_PORTAL_BASE_URL + "?cp=" + cp + "&supplyType=publicLease&cnpCdNmSearchEtc=etc";
                Document doc = (cp == 1) ? firstPageDoc : Jsoup.connect(pageUrl).userAgent(USER_AGENT).timeout(15000).get();
                
                boolean hasData = processPageRows(doc);
                if (!hasData) break;
                
                Thread.sleep(100);
            }
            log.info("LH 수집 완료.");
        } catch (Exception e) {
            log.error("LH 수집 중 오류: {}", e.getMessage());
        }
    }

    private boolean processPageRows(Document doc) {
        Elements rows = doc.select("div.board-list table tbody tr");
        if (rows.isEmpty() || rows.get(0).text().contains("등록된 데이터가 없습니다")) return false;

        for (Element row : rows) {
            try {
                Elements tds = row.select("td");
                if (tds.size() < 8) continue;

                String category = tds.get(1).text().trim();
                String title = tds.get(2).text().trim();
                Element linkAnchor = tds.get(7).select("a").first();
                if (linkAnchor == null) continue;
                
                String detailUrl = linkAnchor.attr("href");
                String apiId = extractSeqFromLhUrl(detailUrl, title);
                
                // 공고게시일 (index 4) 및 마감일 (index 5) 추출
                LocalDateTime annDate = parseDate(tds.get(4).text().trim());
                LocalDateTime deadline = parseDate(tds.get(5).text().trim());

                saveOrUpdateNotice(apiId, title, detailUrl, annDate, deadline, category);
            } catch (Exception e) {
                log.error("LH 개별 공고 파싱 오류: {}", e.getMessage());
            }
        }
        return true;
    }

    private void saveOrUpdateNotice(String apiId, String title, String detailUrl, LocalDateTime annDate, LocalDateTime deadline, String category) {
        Optional<Notice> existing = noticeRepository.findBySourceAndApiId("LH", apiId);
        Notice notice;
        
        if (existing.isPresent()) {
            notice = existing.get();
            notice.setTitle(title);
            notice.setLink(detailUrl);
            notice.setDeadline(deadline); // 사이트 제공 날짜로 업데이트
            noticeRepository.save(notice);
        } else {
            notice = noticeRepository.save(Notice.builder()
                    .apiId(apiId).title(title).content("[LH-" + category + "] " + title)
                    .link(detailUrl).source("LH")
                    .announcementDate(annDate).deadline(deadline).build());
        }

        tagService.addTagsToNotice(notice, List.of("LH", "한국토지주택공사", category), TagType.PROVIDER);
        tagService.autoClassifyAndAddTags(notice, true);
    }

    private int extractTotalPages(Document doc) {
        try {
            Element last = doc.selectFirst("div.paging a[title='마지막 페이지']");
            if (last != null && last.attr("href").contains("cp=")) {
                return Integer.parseInt(last.attr("href").split("cp=")[1].split("&")[0]);
            }
        } catch (Exception ignored) {}
        return 1;
    }

    private String extractSeqFromLhUrl(String url, String title) {
        if (url.contains("panId=")) return "LH_" + url.split("panId=")[1].split("&")[0];
        return "LH_" + title.hashCode();
    }

    private LocalDateTime parseDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.isEmpty() || dateStr.contains("상태")) return null;
            String clean = dateStr.replaceAll("[^0-9-]", "");
            if (clean.length() >= 10) return LocalDate.parse(clean.substring(0, 10)).atStartOfDay();
        } catch (Exception ignored) {}
        return null;
    }
}
