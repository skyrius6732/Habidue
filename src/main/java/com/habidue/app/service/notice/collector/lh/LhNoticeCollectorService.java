package com.habidue.app.service.notice.collector.lh;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.tag.TagType;
import com.habidue.app.repository.notice.NoticeRepository;
import com.habidue.app.service.tag.TagService;
import com.habidue.app.service.notice.event.NoticeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LH 청약센터 기반 공고 크롤러
 * https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LhNoticeCollectorService {

    private final NoticeRepository noticeRepository;
    private final TagService tagService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String LIST_URL   = "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancList.do";
    private static final String DETAIL_URL = "https://apply.lh.or.kr/lhapply/apply/wt/wrtanc/selectWrtancInfo.do";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36";
    private static final int    LIST_COUNT = 100;

    private final AtomicBoolean isRunning     = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicInteger totalPagesCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger processedPagesCount = new java.util.concurrent.atomic.AtomicInteger(0);

    public boolean isRunning()      { return isRunning.get(); }
    public void    stopCollection() { stopRequested.set(true); log.info("LH 수집 중단 요청됨."); }
    public int     getTotalPages()  { return totalPagesCount.get(); }
    public int     getProcessedPages() { return processedPagesCount.get(); }

    /**
     * 정기 수집 (스케줄러용) - 최근 60일치만 수집, 오래된 공고 감지 시 조기 종료
     */
    public void collectAndSaveLhNotices() {
        String startDt = LocalDate.now().minusDays(60).toString();
        LocalDateTime earlyStopBefore = LocalDateTime.now().minusDays(60);
        log.info("LH 정기 수집 시작 (startDt={})...", startDt);
        doCollect(startDt, earlyStopBefore);
        log.info("LH 정기 수집 완료.");
    }

    /**
     * 전체 수집 (운영 초기 1회용) - 날짜 필터 없음, 전체 수집
     */
    public void collectAllLhNotices() {
        String startDt = LocalDate.now().minusYears(5).toString();
        log.info("LH 전체 수집 시작 (초기 1회용, startDt={})...", startDt);
        doCollect(startDt, null);
        log.info("LH 전체 수집 완료.");
    }

    private void doCollect(String startDt, LocalDateTime earlyStopBefore) {
        isRunning.set(true);
        stopRequested.set(false);
        tagService.refreshCache();
        int currPage = 1;
        int totalProcessed = 0;
        String endDt = LocalDate.now().toString();
        try {
            Document firstDoc = fetchListPage(1, startDt, endDt);
            int totalPages = extractTotalPages(firstDoc);
            totalPagesCount.set(totalPages);
            processedPagesCount.set(0);
            log.info("LH 총 {} 페이지 수집 예정", totalPages);

            while (currPage <= totalPages) {
                if (stopRequested.get()) {
                    log.info("LH 수집 중단 ({}/{} 페이지, 누적 {}건).", currPage, totalPages, totalProcessed);
                    break;
                }
                log.info("LH 수집 중: {} / {} 페이지", currPage, totalPages);
                Document doc = (currPage == 1) ? firstDoc : fetchListPage(currPage, startDt, endDt);
                int processed = processPageRows(doc, earlyStopBefore);
                totalProcessed += Math.abs(processed);
                processedPagesCount.set(currPage);
                log.info("LH 수집 중: {} / {} 페이지 완료 (누적 {}건)", currPage, totalPages, totalProcessed);
                if (processed < 0) break;
                currPage++;
                Thread.sleep(200);
            }
            log.info("LH 수집 완료 (총 {}페이지, {}건).", currPage, totalProcessed);
        } catch (Exception e) {
            log.error("LH 수집 중 오류: {}", e.getMessage());
        } finally {
            isRunning.set(false);
            stopRequested.set(false);
        }
    }

    private int extractTotalPages(Document doc) {
        try {
            Element lastBtn = doc.selectFirst("a.bbs_arr.pgeR2");
            if (lastBtn != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("goPaging\\((\\d+)\\)").matcher(lastBtn.attr("onclick"));
                if (m.find()) return Integer.parseInt(m.group(1));
            }
        } catch (Exception ignored) {}
        return 1;
    }

    private Document fetchListPage(int page, String startDt, String endDt) throws Exception {
        String panStDt = (startDt != null && !startDt.isEmpty()) ? startDt.replace("-", "") : "";
        String panEdDt = (endDt != null && !endDt.isEmpty()) ? endDt.replace("-", "") : "";

        Connection conn = Jsoup.connect(LIST_URL)
                .userAgent(USER_AGENT)
                .header("Origin", "https://apply.lh.or.kr")
                .header("Referer", LIST_URL)
                .timeout(15000)
                .method(Connection.Method.POST)
                .data("panId", "")
                .data("ccrCnntSysDsCd", "")
                .data("srchUppAisTpCd", "061339")
                .data("uppAisTpCd", "061339")
                .data("aisTpCd", "")
                .data("srchAisTpCd", "")
                .data("prevListCo", String.valueOf(LIST_COUNT))
                .data("mi", "1026")
                .data("currPage", String.valueOf(page))
                .data("indVal", "N")
                .data("viewType", "")
                .data("netbgn", "")
                .data("srchFilter", "N")
                .data("cnpCd", "")
                .data("panSs", "")
                .data("schTy", "0")
                .data("startDt", startDt != null ? startDt : "")
                .data("endDt", endDt != null ? endDt : "")
                .data("panNm", "")
                .data("listCo", String.valueOf(LIST_COUNT));

        if (page == 1) {
            conn.data("srchY", "Y").data("mvinQf", "0");
        } else {
            int minSn = (page - 2) * LIST_COUNT;
            int maxSn = (page - 1) * LIST_COUNT;
            conn.data("srchY", "N")
                .data("mvinQf", "")
                .data("csCd", "CNP_CD")
                .data("xssChk", "N")
                .data("minSn", String.valueOf(minSn))
                .data("maxSn", String.valueOf(maxSn))
                .data("panStDt", panStDt)
                .data("panEdDt", panEdDt);
        }

        return conn.execute().parse();
    }

    /**
     * 페이지 행 처리. 반환값: 양수 = 처리 건수(계속), 음수 = abs(처리건수)(종료)
     */
    private int processPageRows(Document doc, LocalDateTime earlyStopBefore) {
        Elements rows = doc.select("div.bbs_ListA table tbody tr");
        if (rows.isEmpty() || rows.first().text().contains("등록된")) return -0;

        int count = 0;
        for (Element row : rows) {
            try {
                Elements tds = row.select("td");
                if (tds.size() < 8) continue;

                Element linkEl = tds.get(2).selectFirst("a.wrtancInfoBtn");
                if (linkEl == null) continue;

                String panId   = linkEl.attr("data-id1");
                String dataId2 = linkEl.attr("data-id2");
                String dataId3 = linkEl.attr("data-id3");
                String dataId4 = linkEl.attr("data-id4");

                String title    = linkEl.select("span").text().replaceAll("\\d+일전|새글", "").trim();
                String category = tds.get(1).text().trim();
                String region   = tds.get(3).text().trim();

                LocalDateTime annDate  = parseDate(tds.get(5).text().trim());
                LocalDateTime deadline = parseDate(tds.get(6).text().trim());

                // 정기 수집 시 기준일보다 오래된 공고 감지 → 조기 종료
                if (earlyStopBefore != null && annDate != null && annDate.isBefore(earlyStopBefore)) {
                    log.info("기준일({}) 이전 공고 감지 → 수집 조기 종료", earlyStopBefore.toLocalDate());
                    return -(count + 1);
                }

                // [시니어 조치] LH 상세 페이지 필수 파라미터 정상 매핑 (id3=uppAisTpCd, id4=aisTpCd)
                String link = DETAIL_URL + "?mi=1026&panId=" + panId 
                            + "&ccrCnntSysDsCd=" + dataId2 
                            + "&uppAisTpCd=" + dataId3 
                            + "&aisTpCd=" + dataId4;

                String deadlineHint = null;
                if (deadline == null) {
                    Thread.sleep(300);
                    deadlineHint = fetchDeadlineHintFromDetail(panId, dataId2, dataId3, dataId4);
                }

                saveOrUpdateNotice(panId, title, link, annDate, deadline, deadlineHint, category, region);
                count++;
            } catch (Exception e) {
                log.error("LH 개별 공고 파싱 오류: {}", e.getMessage());
            }
        }
        return count;
    }

    @Transactional
    public void saveOrUpdateNotice(String apiId, String title, String link,
                                     LocalDateTime annDate, LocalDateTime deadline,
                                     String deadlineHint, String category, String region) {
        Optional<Notice> existing = noticeRepository.findBySourceAndApiId("LH", apiId);
        Notice notice;

        if (existing.isPresent()) {
            notice = existing.get();
            notice.setTitle(title);
            notice.setLink(link);
            if (deadline != null)     notice.setDeadline(deadline);
            if (deadlineHint != null) notice.setDeadlineHint(deadlineHint);
            noticeRepository.save(notice);
        } else {
            notice = noticeRepository.save(Notice.builder()
                    .apiId(apiId)
                    .title(title)
                    .content("[LH-" + category + "] " + title)
                    .link(link)
                    .source("LH")
                    .announcementDate(annDate)
                    .deadline(deadline)
                    .deadlineHint(deadlineHint)
                    .build());
            eventPublisher.publishEvent(new NoticeCreatedEvent(notice));
        }

        List<String> providerTags = new ArrayList<>(List.of("LH", "한국토지주택공사", category));
        if (region != null && !region.isBlank()) providerTags.add(region);
        tagService.applyTagsToNotice(notice, providerTags, existing.isEmpty());
    }

    private String fetchDeadlineHintFromDetail(String panId, String dataId2, String dataId3, String dataId4) {
        try {
            Document doc = Jsoup.connect(DETAIL_URL)
                    .userAgent(USER_AGENT)
                    .header("Referer", LIST_URL)
                    .timeout(15000)
                    .method(Connection.Method.POST)
                    .data("mi", "1026")
                    .data("panId", panId)
                    .data("id2", dataId2)
                    .data("id3", dataId3)
                    .data("id4", dataId4)
                    .execute()
                    .parse();

            for (Element el : doc.select("th, td, p, li")) {
                String text = el.text();
                if (text.contains("접수기간") || text.contains("신청기간") || text.contains("모집기간")) {
                    String hint = text.trim();
                    return hint.length() > 200 ? hint.substring(0, 200) : hint;
                }
            }
        } catch (Exception e) {
            log.warn("LH 상세 마감일 추출 실패 (panId={}): {}", panId, e.getMessage());
        }
        return null;
    }

    private LocalDateTime parseDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.isBlank() || dateStr.equals("-")) return null;
            String clean = dateStr.replaceAll("[^0-9.]", "").replace(".", "-");
            if (clean.length() >= 10) return LocalDate.parse(clean.substring(0, 10)).atStartOfDay();
        } catch (Exception ignored) {}
        return null;
    }
}
