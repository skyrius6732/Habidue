package com.habidue.app.service.notice.collector.sh;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShNoticeCrawlerService {

    private final NoticeRepository noticeRepository;
    private final TagService tagService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String SH_PORTAL_BASE_URL = "https://housing.seoul.go.kr/site/main/sh/publicLease/list";
    private static final String BASE_URL = "https://housing.seoul.go.kr";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";

    private final AtomicBoolean isRunning     = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicInteger totalPagesCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger processedPagesCount = new java.util.concurrent.atomic.AtomicInteger(0);

    public boolean isRunning()      { return isRunning.get(); }
    public void    stopCollection() { stopRequested.set(true); log.info("SH 수집 중단 요청됨."); }
    public int     getTotalPages()  { return totalPagesCount.get(); }
    public int     getProcessedPages() { return processedPagesCount.get(); }

    /** 정기 수집 (스케줄러용) */
    public void crawlShNotices() {
        log.info("SH 공고 수집 시작...");
        doCollect();
        log.info("SH 공고 수집 완료.");
    }

    /** 전체 수집 (운영 초기 1회용) */
    public void crawlAllShNotices() {
        log.info("SH 전체 수집 시작 (초기 1회용)...");
        doCollect();
        log.info("SH 전체 수집 완료.");
    }

    private void doCollect() {
        isRunning.set(true);
        stopRequested.set(false);
        int totalProcessed = 0;
        try {
            Document firstPageDoc = Jsoup.connect(SH_PORTAL_BASE_URL).userAgent(USER_AGENT).timeout(15000).get();
            int totalPages = extractTotalPages(firstPageDoc);
            totalPagesCount.set(totalPages);
            processedPagesCount.set(0);
            log.info("SH 총 {} 페이지 수집 예정", totalPages);

            for (int cp = 1; cp <= totalPages; cp++) {
                if (stopRequested.get()) {
                    log.info("SH 수집 중단 ({}페이지, 누적 {}건).", cp, totalProcessed);
                    break;
                }
                log.info("SH 수집 중: {} / {} 페이지", cp, totalPages);
                String pageUrl = SH_PORTAL_BASE_URL + "?cp=" + cp + "&supplyType=publicLease";
                Document doc = (cp == 1) ? firstPageDoc : Jsoup.connect(pageUrl).userAgent(USER_AGENT).timeout(15000).get();

                int count = processPageRows(doc);
                totalProcessed += count;
                processedPagesCount.set(cp);
                log.info("SH 수집 중: {} / {} 페이지 완료 (누적 {}건)", cp, totalPages, totalProcessed);
                if (count == 0) break;

                Thread.sleep(300);
            }
            log.info("SH 수집 완료 (총 {}건).", totalProcessed);
        } catch (Exception e) {
            log.error("SH 수집 중 오류: {}", e.getMessage());
        } finally {
            isRunning.set(false);
            stopRequested.set(false);
        }
    }

    private int processPageRows(Document doc) {
        Elements rows = doc.select("div.board-list table tbody tr");
        if (rows.isEmpty() || rows.get(0).text().contains("등록된 데이터가 없습니다")) return 0;

        int count = 0;
        for (Element row : rows) {
            try {
                Elements tds = row.select("td");
                if (tds.size() < 7) continue;

                String category = tds.get(1).text().trim();
                String title = tds.get(2).text().trim();

                Element linkAnchor = tds.get(6).select("a").first();
                if (linkAnchor == null) continue;

                String href = linkAnchor.attr("href");
                String detailUrl = href.startsWith("http") ? href : BASE_URL + href;
                String apiId = extractSeq(detailUrl, title);

                LocalDateTime annDate    = parseDate(tds.get(3).text().trim());
                LocalDateTime resultDate = parseDate(tds.get(4).text().trim());
                LocalDateTime deadline   = fetchAndExtractDeadline(detailUrl, annDate, resultDate);

                saveOrUpdateNotice(apiId, title, detailUrl, annDate, deadline, resultDate, category);
                count++;
            } catch (Exception e) {
                log.error("SH 개별 공고 파싱 오류: {}", e.getMessage());
            }
        }
        return count;
    }

    @Transactional
    public void saveOrUpdateNotice(String apiId, String title, String detailUrl,
                                    LocalDateTime annDate, LocalDateTime deadline,
                                    LocalDateTime resultDate, String category) {
        Optional<Notice> existing = noticeRepository.findBySourceAndApiId("SH", apiId);
        boolean isNew = existing.isEmpty();
        Notice notice;

        if (!isNew) {
            notice = existing.get();
            notice.setTitle(title);
            notice.setLink(detailUrl);
            notice.setDeadline(deadline);
            notice.setResultDate(resultDate);
            noticeRepository.save(notice);
        } else {
            notice = noticeRepository.save(Notice.builder()
                    .apiId(apiId).title(title).content("[" + category + "] " + title)
                    .link(detailUrl).source("SH")
                    .announcementDate(annDate).deadline(deadline).resultDate(resultDate).build());
        }

        tagService.applyTagsToNotice(notice, List.of("SH", "서울주택도시공사", category), isNew);
    }

    private LocalDateTime fetchAndExtractDeadline(String url, LocalDateTime annDate, LocalDateTime resultDate) {
        try {
            Document detailDoc = Jsoup.connect(url).userAgent(USER_AGENT).timeout(10000).get();
            detailDoc.outputSettings(new Document.OutputSettings().prettyPrint(false));

            Element contentBox = detailDoc.selectFirst("div.board-view-cont");
            if (contentBox == null) contentBox = detailDoc.selectFirst("div.cont");

            String text;
            if (contentBox != null) {
                contentBox.select("br").append("\\n");
                contentBox.select("p").prepend("\\n");
                contentBox.select("tr").append("\\n");
                text = contentBox.text();
            } else {
                text = detailDoc.text();
            }

            String cleanText = text.replace("～", "~").replace("∼", "~").replace("－", "-").replace("—", "-");

            Pattern fullDatePattern  = Pattern.compile("(\\d{4})[\\.\\s\\-]*(\\d{1,2})[\\.\\s\\-]*(\\d{1,2})");
            Pattern shortDatePattern = Pattern.compile("(\\d{1,2})[\\.\\s\\-]*(\\d{1,2})");
            Pattern pattern = Pattern.compile("(?:인터넷\\s?(?:청약)?접수|방문\\s?접수|신청기간|접수기간)\\s*[:\\s]*([^■●○▶*]{5,200}?(?=\\s*(?:■|●|○|▶|\\*|모집|발표|서류|계약|공고|입주|$)))");
            Matcher matcher = pattern.matcher(cleanText);

            while (matcher.find()) {
                String section = matcher.group(1).trim();
                if ((!fullDatePattern.matcher(section).find() && !shortDatePattern.matcher(section).find())
                        || section.contains("조회수") || section.contains("등록일")) continue;

                if (section.contains("~")) {
                    String[] parts = section.split("~");
                    if (parts.length > 1) {
                        String endPart = parts[1].trim();
                        Matcher m = fullDatePattern.matcher(endPart);
                        if (m.find()) return createLocalDateTime(m);
                        Matcher m2 = shortDatePattern.matcher(endPart);
                        if (m2.find()) {
                            Matcher mStart = fullDatePattern.matcher(parts[0]);
                            int year = mStart.find() ? Integer.parseInt(mStart.group(1)) : (annDate != null ? annDate.getYear() : LocalDate.now().getYear());
                            if (year < 100) year += 2000;
                            return LocalDate.of(year, Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2))).atStartOfDay();
                        }
                    }
                }

                Matcher m = fullDatePattern.matcher(section);
                LocalDateTime lastDate = null;
                while (m.find()) lastDate = createLocalDateTime(m);
                if (lastDate != null) return lastDate;
            }
        } catch (Exception e) {
            log.warn("SH 상세 페이지 분석 실패 ({}): {}", url, e.getMessage());
        }
        return resultDate;
    }

    private LocalDateTime createLocalDateTime(Matcher dm) {
        int year = Integer.parseInt(dm.group(1));
        if (year < 100) year += 2000;
        return LocalDate.of(year, Integer.parseInt(dm.group(2)), Integer.parseInt(dm.group(3))).atStartOfDay();
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

    private String extractSeq(String url, String title) {
        if (url.contains("seq=")) return "SH_" + url.split("seq=")[1].split("&")[0];
        return "SH_" + title.hashCode();
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            Pattern p = Pattern.compile("(\\d{4})[\\.\\-\\s]+(\\d{1,2})[\\.\\-\\s]+(\\d{1,2})");
            Matcher m = p.matcher(dateStr);
            if (m.find()) return LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))).atStartOfDay();
            return LocalDate.parse(dateStr.substring(0, 10)).atStartOfDay();
        } catch (Exception e) { return null; }
    }
}
