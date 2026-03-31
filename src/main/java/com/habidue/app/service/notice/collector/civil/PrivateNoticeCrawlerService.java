package com.habidue.app.service.notice.collector.civil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.tag.TagType;
import com.habidue.app.repository.notice.NoticeRepository;
import com.habidue.app.service.tag.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateNoticeCrawlerService {

    private final NoticeRepository noticeRepository;
    private final TagService tagService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String API_URL      = "https://soco.seoul.go.kr/youth/pgm/home/yohome/bbsListJson.json";
    private static final String VIEW_BASE_URL = "https://soco.seoul.go.kr/youth/bbs/BMSR00015/view.do?menuNo=400008&boardId=";

    private final AtomicBoolean isRunning     = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicInteger totalPagesCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger processedPagesCount = new java.util.concurrent.atomic.AtomicInteger(0);

    public boolean isRunning()      { return isRunning.get(); }
    public void    stopCollection() { stopRequested.set(true); log.info("민간임대 수집 중단 요청됨."); }
    public int     getTotalPages()  { return totalPagesCount.get(); }
    public int     getProcessedPages() { return processedPagesCount.get(); }

    /** 정기 수집 (스케줄러용) */
    public void crawlPrivateNotices() {
        log.info("민간임대 공고 수집 시작...");
        doCollect();
        log.info("민간임대 공고 수집 완료.");
    }

    /** 전체 수집 (운영 초기 1회용) */
    public void crawlAllPrivateNotices() {
        log.info("민간임대 전체 수집 시작 (초기 1회용)...");
        doCollect();
        log.info("민간임대 전체 수집 완료.");
    }

    private void doCollect() {
        isRunning.set(true);
        stopRequested.set(false);
        int totalProcessed = 0;
        try {
            int totalPages = 1;
            for (int page = 1; page <= totalPages; page++) {
                if (stopRequested.get()) {
                    log.info("민간임대 수집 중단 ({}페이지, 누적 {}건).", page, totalProcessed);
                    break;
                }
                log.info("민간임대 수집 중: {} / {} 페이지", page, totalPages);

                String responseBody = fetchJson(page);
                JsonNode root = objectMapper.readTree(responseBody);

                if (page == 1) {
                    totalPages = root.path("pagingInfo").path("totPage").asInt(1);
                    totalPagesCount.set(totalPages);
                    processedPagesCount.set(0);
                }

                JsonNode list = root.path("resultList");
                int count = 0;
                for (JsonNode node : list) {
                    processNotice(node);
                    count++;
                }
                totalProcessed += count;
                processedPagesCount.set(page);
                log.info("민간임대 수집 중: {} / {} 페이지 완료 (누적 {}건)", page, totalPages, totalProcessed);

                Thread.sleep(500);
            }
            log.info("민간임대 수집 완료 (총 {}건).", totalProcessed);
        } catch (Exception e) {
            log.error("민간임대 수집 중 오류: {}", e.getMessage());
        } finally {
            isRunning.set(false);
            stopRequested.set(false);
        }
    }

    private String fetchJson(int page) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("bbsId", "BMSR00015");
        map.add("pageIndex", String.valueOf(page));
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        return restTemplate.postForObject(API_URL, request, String.class);
    }

    @Transactional
    public void processNotice(JsonNode node) {
        String boardId      = node.path("boardId").asText();
        String title        = node.path("nttSj").asText();
        String contentHtml  = node.path("content").asText();
        String annDateStr   = node.path("optn1").asText();
        String deadlineHint = node.path("optn4").asText();

        String apiId      = "PRIVATE_" + boardId;
        String detailUrl  = VIEW_BASE_URL + boardId;

        LocalDateTime announcementDate = parseDate(annDateStr);
        LocalDateTime deadline         = extractDeadline(contentHtml, deadlineHint);

        Optional<Notice> existing = noticeRepository.findBySourceAndApiId("PRIVATE", apiId);
        boolean isNew = existing.isEmpty();
        Notice notice;

        if (!isNew) {
            notice = existing.get();
            boolean changed = false;
            if (!notice.getTitle().equals(title)) { notice.setTitle(title); changed = true; }
            if (deadline != null && (notice.getDeadline() == null || !notice.getDeadline().equals(deadline))) {
                notice.setDeadline(deadline); changed = true;
            }
            if (changed) notice = noticeRepository.save(notice);
        } else {
            notice = noticeRepository.save(Notice.builder()
                    .source("PRIVATE").apiId(apiId).title(title)
                    .content("[민간임대] " + title).link(detailUrl)
                    .announcementDate(announcementDate).deadline(deadline).build());
        }

        tagService.applyTagsToNotice(notice, List.of("민간임대", "청년안심주택", "서울시"), isNew);
    }

    private LocalDateTime extractDeadline(String html, String hint) {
        try {
            String text = Jsoup.parse(html).text();
            Pattern pattern = Pattern.compile("(?:신청|청약신청|인터넷접수|접수기간|청약기간)\\s*[:\\s]*([^■●○▶*\\-]{5,100}?(?=\\s*(?:■|●|○|▶|\\*|-|모집|발표|서류|계약|공고|입주|$)))");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                String section = matcher.group(1).trim();
                Pattern fullDatePattern  = Pattern.compile("(\\d{2,4})[\\.\\-\\s]+(\\d{1,2})[\\.\\-\\s]+(\\d{1,2})");
                Pattern shortDatePattern = Pattern.compile("(\\d{1,2})[\\.\\-\\s]+(\\d{1,2})");

                if (section.contains("~")) {
                    String[] parts = section.split("~");
                    if (parts.length > 1) {
                        String endPart = parts[1].trim();
                        Matcher m = fullDatePattern.matcher(endPart);
                        if (m.find()) return createLocalDateTime(m);
                        Matcher m2 = shortDatePattern.matcher(endPart);
                        if (m2.find()) {
                            Matcher mStart = fullDatePattern.matcher(parts[0]);
                            int year = mStart.find() ? Integer.parseInt(mStart.group(1)) : LocalDate.now().getYear();
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
            log.warn("민간임대 마감일 파싱 실패: {}", e.getMessage());
        }
        return parseDate(hint);
    }

    private LocalDateTime createLocalDateTime(Matcher dm) {
        int year = Integer.parseInt(dm.group(1));
        if (year < 100) year += 2000;
        return LocalDate.of(year, Integer.parseInt(dm.group(2)), Integer.parseInt(dm.group(3))).atStartOfDay();
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.length() < 10) return null;
        try {
            return LocalDate.parse(dateStr.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (Exception e) { return null; }
    }
}
