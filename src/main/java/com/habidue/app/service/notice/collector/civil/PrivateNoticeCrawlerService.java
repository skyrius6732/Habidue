package com.habidue.app.service.notice.collector.civil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.tag.TagType;
import com.habidue.app.repository.notice.NoticeRepository;
import com.habidue.app.service.tag.TagService;
import com.habidue.app.service.notice.event.NoticeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 민간임대 (서울시 청년안심주택) 공고 크롤러 - 고도화 버전
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateNoticeCrawlerService {

    private final NoticeRepository noticeRepository;
    private final TagService tagService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String API_URL = "https://soco.seoul.go.kr/youth/pgm/home/yohome/bbsListJson.json";
    private static final String VIEW_BASE_URL = "https://soco.seoul.go.kr/youth/bbs/BMSR00015/view.do?menuNo=400008&boardId=";

    @Transactional
    public void crawlPrivateNotices() {
        log.info("민간임대(청년안심주택) 공고 수집 시작 (태그 고도화 적용)...");
        int totalPages = 1;

        try {
            for (int page = 1; page <= totalPages; page++) {
                log.info("민간임대 수집 중: {} / {} 페이지", page, totalPages);
                
                String responseBody = fetchJson(page);
                JsonNode root = objectMapper.readTree(responseBody);
                
                if (page == 1) {
                    totalPages = root.path("pagingInfo").path("totPage").asInt(1);
                }

                JsonNode list = root.path("resultList");
                for (JsonNode node : list) {
                    processNotice(node);
                }
                
                Thread.sleep(500); 
            }
            log.info("민간임대 고도화 수집 완료.");
        } catch (Exception e) {
            log.error("민간임대 수집 중 오류: {}", e.getMessage());
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

    private void processNotice(JsonNode node) {
        String boardId = node.path("boardId").asText();
        String title = node.path("nttSj").asText();
        String contentHtml = node.path("content").asText();
        String annDateStr = node.path("optn1").asText(); 
        String deadlineHint = node.path("optn4").asText(); 

        String apiId = "PRIVATE_" + boardId;
        String detailUrl = VIEW_BASE_URL + boardId;

        LocalDateTime announcementDate = parseDate(annDateStr);
        LocalDateTime deadline = extractDeadline(contentHtml, deadlineHint);

        Optional<Notice> existing = noticeRepository.findBySourceAndApiId("PRIVATE", apiId);
        Notice notice;

        if (existing.isPresent()) {
            notice = existing.get();
            boolean changed = false;
            if (!notice.getTitle().equals(title)) { notice.setTitle(title); changed = true; }
            if (deadline != null && (notice.getDeadline() == null || !notice.getDeadline().equals(deadline))) {
                notice.setDeadline(deadline);
                changed = true;
            }
            if (changed) notice = noticeRepository.save(notice);
        } else {
            notice = noticeRepository.save(Notice.builder()
                    .source("PRIVATE").apiId(apiId).title(title)
                    .content("[민간임대] " + title).link(detailUrl)
                    .announcementDate(announcementDate).deadline(deadline).build());
        }

        // [최적화] 태그 저장을 TagService의 중앙 엔진으로 통합
        tagService.addTagsToNotice(notice, List.of("민간임대", "청년안심주택", "서울시"), TagType.PROVIDER);
        
        // [핵심] 제목 및 본문 기반의 복합 분석(역 이름, 지역구 매핑, 상태 판별 등)을 중앙 엔진에 위임
        tagService.autoClassifyAndAddTags(notice, true);
    }

    private LocalDateTime extractDeadline(String html, String hint) {
        try {
            String text = Jsoup.parse(html).text();
            // 키워드 확장 및 다음 섹션(기호 또는 주요 키워드) 전까지만 탐색
            // ■, ●, ○, ▶, *, - 기호 또는 '발표', '서류', '계약', '모집' 단어 앞에서 중단 (Lookahead 적용)
            Pattern pattern = Pattern.compile("(?:신청|청약신청|인터넷접수|접수기간|청약기간)\\s*[:\\s]*([^■●○▶*\\-]{5,100}?(?=\\s*(?:■|●|○|▶|\\*|-|모집|발표|서류|계약|공고|입주|$)))");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                String section = matcher.group(1).trim();
                
                // 날짜 패턴 정의
                Pattern fullDatePattern = Pattern.compile("(\\d{2,4})[\\.\\-\\s]+(\\d{1,2})[\\.\\-\\s]+(\\d{1,2})");
                Pattern shortDatePattern = Pattern.compile("(\\d{1,2})[\\.\\-\\s]+(\\d{1,2})");

                // 물결표(~)가 있다면 마감일인 뒷부분을 우선적으로 분석
                if (section.contains("~")) {
                    String[] parts = section.split("~");
                    if (parts.length > 1) {
                        String endPart = parts[1].trim();
                        
                        // 1순위: 뒷부분에서 전체 날짜(Y.M.D) 추출
                        Matcher m = fullDatePattern.matcher(endPart);
                        if (m.find()) return createLocalDateTime(m);
                        
                        // 2순위: 뒷부분에서 월.일(M.D) 추출 (연도는 앞부분에서 상속)
                        Matcher m2 = shortDatePattern.matcher(endPart);
                        if (m2.find()) {
                            Matcher mStart = fullDatePattern.matcher(parts[0]);
                            int year = mStart.find() ? Integer.parseInt(mStart.group(1)) : LocalDate.now().getYear();
                            if (year < 100) year += 2000;
                            return LocalDate.of(year, Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2))).atStartOfDay();
                        }
                    }
                }

                // 3순위: 물결표가 없거나 위에서 실패한 경우 해당 섹션의 마지막 전체 날짜 추출
                Matcher m = fullDatePattern.matcher(section);
                LocalDateTime lastDate = null;
                while (m.find()) {
                    lastDate = createLocalDateTime(m);
                }
                if (lastDate != null) return lastDate;
            }
        } catch (Exception e) {
            log.warn("Text parsing failed for deadline, using hint: {}", e.getMessage());
        }
        return parseDate(hint);
    }

    private LocalDateTime createLocalDateTime(Matcher dm) {
        int year = Integer.parseInt(dm.group(1));
        if (year < 100) year += 2000;
        int month = Integer.parseInt(dm.group(2));
        int day = Integer.parseInt(dm.group(3));
        return LocalDate.of(year, month, day).atStartOfDay();
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.length() < 10) return null;
        try {
            return LocalDate.parse(dateStr.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (Exception e) { return null; }
    }
}
