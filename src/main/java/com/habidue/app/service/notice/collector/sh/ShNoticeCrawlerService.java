package com.habidue.app.service.notice.collector.sh;

import com.habidue.app.domain.notice.Notice;
import com.habidue.app.domain.tag.TagType;
import com.habidue.app.repository.notice.NoticeRepository;
import com.habidue.app.service.tag.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 서울주거포털 기반 SH 공고 크롤러 (상세 페이지 분석 고도화 및 날짜 파싱 강화)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShNoticeCrawlerService {

    private final NoticeRepository noticeRepository;
    private final TagService tagService;

    private static final String SH_PORTAL_BASE_URL = "https://housing.seoul.go.kr/site/main/sh/publicLease/list";
    private static final String BASE_URL = "https://housing.seoul.go.kr";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";

    @Transactional
    public void crawlShNotices() {
        log.info("서울주거포털 SH 공고 수집 시작 (날짜 파싱 강화 적용)...");
        try {
            Document firstPageDoc = Jsoup.connect(SH_PORTAL_BASE_URL).userAgent(USER_AGENT).timeout(15000).get();
            int totalPages = extractTotalPages(firstPageDoc);
            log.info("총 {} 페이지 수집 예정", totalPages);

            for (int cp = 1; cp <= totalPages; cp++) {
                log.info("SH 수집 중: {} / {} 페이지", cp, totalPages);
                String pageUrl = SH_PORTAL_BASE_URL + "?cp=" + cp + "&supplyType=publicLease";
                Document doc = (cp == 1) ? firstPageDoc : Jsoup.connect(pageUrl).userAgent(USER_AGENT).timeout(15000).get();
                
                boolean hasData = processPageRows(doc);
                if (!hasData) break;
                
                Thread.sleep(300); 
            }
            log.info("SH 고도화 수집 완료.");
        } catch (Exception e) {
            log.error("SH 수집 중 치명적 오류: {}", e.getMessage());
        }
    }

    private boolean processPageRows(Document doc) {
        Elements rows = doc.select("div.board-list table tbody tr");
        if (rows.isEmpty() || rows.get(0).text().contains("등록된 데이터가 없습니다")) return false;

        for (Element row : rows) {
            try {
                Elements tds = row.select("td");
                if (tds.size() < 7) continue;

                String category = tds.get(1).text().trim();
                String title = tds.get(2).text().trim();
                log.info("  > 공고 분석 중: {}", title.length() > 25 ? title.substring(0, 22) + "..." : title);

                Element linkAnchor = tds.get(6).select("a").first();
                if (linkAnchor == null) continue;
                
                String href = linkAnchor.attr("href");
                String detailUrl = href.startsWith("http") ? href : BASE_URL + href;
                String apiId = extractSeq(detailUrl, title);
                
                LocalDateTime annDate = parseDate(tds.get(3).text().trim());
                LocalDateTime resultDate = parseDate(tds.get(4).text().trim());

                // 상세 페이지 방문하여 실제 마감일 추출
                LocalDateTime actualDeadline = fetchAndExtractDeadline(detailUrl, annDate, resultDate);

                saveOrUpdateNotice(apiId, title, detailUrl, annDate, actualDeadline, resultDate, category);
            } catch (Exception e) {
                log.error("SH 개별 공고 파싱 오류: {}", e.getMessage());
            }
        }
        return true;
    }

    private LocalDateTime fetchAndExtractDeadline(String url, LocalDateTime annDate, LocalDateTime resultDate) {
        try {
            Document detailDoc = Jsoup.connect(url).userAgent(USER_AGENT).timeout(10000).get();
            // 줄바꿈 보존 설정
            detailDoc.outputSettings(new Document.OutputSettings().prettyPrint(false));
            
            // [핵심] 상단 레이아웃 노이즈를 피하기 위해 순수 본문 영역만 타겟팅
            Element contentBox = detailDoc.selectFirst("div.board-view-cont");
            if (contentBox == null) contentBox = detailDoc.selectFirst("div.cont");
            
            // 본문 박스가 없으면 전체에서 찾되, 본문 박스가 있으면 그 안에서만 찾음
            String text;
            if (contentBox != null) {
                contentBox.select("br").append("\\n");
                contentBox.select("p").prepend("\\n");
                contentBox.select("tr").append("\\n");
                text = contentBox.text();
            } else {
                text = detailDoc.text();
            }
            
            // 노이즈 제거: 등록일, 조회수 등이 포함된 상단 정보 차단 (이미 contentBox 타겟팅으로 상당수 제거됨)
            String cleanText = text.replace("～", "~").replace("∼", "~").replace("－", "-").replace("—", "-");

            // 날짜 패턴
            Pattern fullDatePattern = Pattern.compile("(\\d{4})[\\.\\s\\-]*(\\d{1,2})[\\.\\s\\-]*(\\d{1,2})");
            Pattern shortDatePattern = Pattern.compile("(\\d{1,2})[\\.\\s\\-]*(\\d{1,2})");

            // 마감일 키워드 탐색
            Pattern pattern = Pattern.compile("(?:인터넷\\s?(?:청약)?접수|방문\\s?접수|신청기간|접수기간)\\s*[:\\s]*([^■●○▶*]{5,200}?(?=\\s*(?:■|●|○|▶|\\*|모집|발표|서류|계약|공고|입주|$)))");
            Matcher matcher = pattern.matcher(cleanText);

            while (matcher.find()) {
                String section = matcher.group(1).trim();
                
                // 날짜가 없거나 '조회수', '등록일' 같은 노이즈가 섞인 섹션은 스킵
                if ((!fullDatePattern.matcher(section).find() && !shortDatePattern.matcher(section).find()) 
                    || section.contains("조회수") || section.contains("등록일")) {
                    continue;
                }

                log.debug("    [확정 분석 섹션]: {}", section);

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
        int month = Integer.parseInt(dm.group(2));
        int day = Integer.parseInt(dm.group(3));
        return LocalDate.of(year, month, day).atStartOfDay();
    }

    private void saveOrUpdateNotice(String apiId, String title, String detailUrl, LocalDateTime annDate, LocalDateTime deadline, LocalDateTime resultDate, String category) {
        Optional<Notice> existing = noticeRepository.findBySourceAndApiId("SH", apiId);
        Notice notice;
        
        if (existing.isPresent()) {
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

        tagService.addTagsToNotice(notice, List.of("SH", "서울주택도시공사", category), TagType.PROVIDER);
        tagService.autoClassifyAndAddTags(notice);
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
            // 숫자만 추출 (2025.2.21 -> 2025, 2, 21)
            Pattern p = Pattern.compile("(\\d{4})[\\.\\-\\s]+(\\d{1,2})[\\.\\-\\s]+(\\d{1,2})");
            Matcher m = p.matcher(dateStr);
            if (m.find()) {
                return LocalDate.of(
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3))
                ).atStartOfDay();
            }
            // 하이픈 포맷 시도 (yyyy-MM-dd)
            return LocalDate.parse(dateStr.substring(0, 10)).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }
}
