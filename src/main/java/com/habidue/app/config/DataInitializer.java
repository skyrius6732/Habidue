package com.habidue.app.config;

import com.habidue.app.repository.notice.NoticeKeywordMetadataRepository;
import com.habidue.app.service.notice.collector.lh.LhNoticeCollectorService;
import com.habidue.app.service.notice.collector.civil.PrivateNoticeCrawlerService;
import com.habidue.app.service.notice.collector.sh.ShNoticeCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final LhNoticeCollectorService lhNoticeCollectorService;
    private final ShNoticeCrawlerService shNoticeCrawlerService;
    private final PrivateNoticeCrawlerService privateNoticeCrawlerService;
    private final com.habidue.app.service.tag.TagService tagService;
    private final NoticeKeywordMetadataRepository metadataRepository;
    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        // 키워드 메타데이터 초기화 (INSERT IGNORE로 중복 건너뜀, 신규 데이터만 추가)
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("notice_keyword_metadata.sql"));
            log.info("키워드 메타데이터 동기화 완료. (현재 {}건)", metadataRepository.count());
        } catch (Exception e) {
            log.error("키워드 메타데이터 동기화 실패: {}", e.getMessage());
        }

        log.info("애플리케이션 시작: 초기 공고 데이터 수집을 진행합니다.");
        
        try {
            // LH 공고 수집
//            lhNoticeCollectorService.collectAndSaveLhNotices();
        } catch (Exception e) {
            log.error("초기 LH 공고 수집 중 오류 발생: {}", e.getMessage());
        }

        try {
            // SH 공고 수집
//            shNoticeCrawlerService.crawlShNotices(); // 실행시 sh 데이터 수집
        } catch (Exception e) {
            log.error("초기 SH 공고 수집 중 오류 발생: {}", e.getMessage());
        }

        try {
            // 민간임대(청년안심주택) 공고 수집
//            privateNoticeCrawlerService.crawlPrivateNotices();
        } catch (Exception e) {
            log.error("초기 민간임대 공고 수집 중 오류 발생: {}", e.getMessage());
        }

        // [추가] 기존에 수집된 공고 중 태그가 없는 데이터(SH 등) 보정 및 상태 동기화
        try {
            log.info("기존 공고 태그 보정 및 상태 동기화 시작...");
//            tagService.syncSystemTags();

        } catch (Exception e) {
            log.error("태그 동기화 중 오류 발생: {}", e.getMessage());
        }

        log.info("초기 데이터 수집 완료.");
    }
}
