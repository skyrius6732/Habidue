package com.habidue.app.config;

import com.habidue.app.domain.badge.Badge;
import com.habidue.app.domain.badge.BadgeLevelRule;
import com.habidue.app.repository.badge.BadgeRepository;
import com.habidue.app.repository.badge.BadgeLevelRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BadgeInitializer implements CommandLineRunner {

    private final BadgeRepository badgeRepository;
    private final BadgeLevelRuleRepository badgeLevelRuleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initializeBadges();
        initializeBadgeRules();
    }

    private void initializeBadges() {
        List<Badge> baseBadges = Arrays.asList(
            Badge.builder().code("KNOWLEDGE_BASE").name("주택 지식인").description("도움이 되는 정보를 공유하는 유저").type("KNOWLEDGE").level(1).build(),
            Badge.builder().code("COLLECTOR_BASE").name("공고 컬렉터").description("공고를 적극적으로 탐색하는 유저").type("COLLECTOR").level(1).build(),
            Badge.builder().code("REVIEW_BASE").name("후기 작가").description("소중한 경험을 기록으로 남기는 유저").type("REVIEW").level(1).build(),
            Badge.builder().code("COMMUNITY_BASE").name("커뮤니티 활동가").description("다양한 소식을 공유하며 활동하는 유저").type("COMMUNITY").level(1).build(),
            Badge.builder().code("COMMUNICATOR_BASE").name("소통의 달인").description("이웃들과 활발히 대화하는 유저").type("COMMUNICATOR").level(1).build(),
            Badge.builder().code("ATTENDANCE_BASE").name("프로 참석러").description("매일매일 꾸준히 방문하는 유저").type("ATTENDANCE").level(1).build()
        );

        for (Badge badge : baseBadges) {
            if (badgeRepository.findByCode(badge.getCode()).isEmpty()) {
                badgeRepository.save(badge);
            }
        }
    }

    private void initializeBadgeRules() {
        if (badgeLevelRuleRepository.count() > 0) return;

        String[] types = {"KNOWLEDGE", "COLLECTOR", "REVIEW", "COMMUNITY", "COMMUNICATOR", "ATTENDANCE"};
        int[] levels = {1, 5, 10, 20, 50, 70, 90, 100};
        String[] rankEmojis = {"🌱", "🔰", "🥉", "🥈", "🥇", "🏅", "🏆", "👑"};

        for (String type : types) {
            for (int i = 0; i < levels.length; i++) {
                int lv = levels[i];
                String emoji = rankEmojis[i];
                
                String rTitle = switch(lv) {
                    case 1 -> "새싹"; case 5 -> "입문"; case 10 -> "활동";
                    case 20 -> "멤버"; case 50 -> "핵심"; case 70 -> "리더";
                    case 90 -> "전설의"; case 100 -> "지배자"; default -> "활동가";
                };

                String cName = switch(type) {
                    case "KNOWLEDGE" -> "지식인";
                    case "COLLECTOR" -> "컬렉터";
                    case "REVIEW" -> lv >= 50 ? "후기작가" : "후기요정";
                    case "COMMUNITY" -> "활동가";
                    case "COMMUNICATOR" -> "소통러";
                    case "ATTENDANCE" -> "참석러";
                    default -> "활동가";
                };

                if (lv == 100) {
                    rTitle = ""; 
                    cName = switch(type) {
                        case "KNOWLEDGE" -> "지식의 군주";
                        case "COLLECTOR" -> "컬렉터의 제왕";
                        case "REVIEW" -> "후기의 지배자";
                        case "COMMUNITY" -> "커뮤니티의 거성";
                        case "COMMUNICATOR" -> "소통의 대가";
                        case "ATTENDANCE" -> "출석의 화신";
                        default -> "전설";
                    };
                }

                badgeLevelRuleRepository.save(BadgeLevelRule.builder()
                        .badgeType(type)
                        .level(lv)
                        .requiredValue(getRequiredValueForInit(type, lv))
                        .rankEmoji(emoji)
                        .rankTitle(rTitle)
                        .categoryName(cName)
                        .build());
            }
        }
    }

    private int getRequiredValueForInit(String type, int level) {
        if (level >= 100) return switch(type) { case "KNOWLEDGE" -> 2000; case "COLLECTOR" -> 1000; case "REVIEW" -> 300; case "COMMUNITY" -> 500; case "COMMUNICATOR" -> 2000; case "ATTENDANCE" -> 500; default -> 9999; };
        if (level >= 90) return switch(type) { case "KNOWLEDGE" -> 1000; case "COLLECTOR" -> 500; case "REVIEW" -> 150; case "COMMUNITY" -> 250; case "COMMUNICATOR" -> 1000; case "ATTENDANCE" -> 365; default -> 9999; };
        if (level >= 70) return switch(type) { case "KNOWLEDGE" -> 500; case "COLLECTOR" -> 250; case "REVIEW" -> 80; case "COMMUNITY" -> 150; case "COMMUNICATOR" -> 500; case "ATTENDANCE" -> 200; default -> 9999; };
        if (level >= 50) return switch(type) { case "KNOWLEDGE" -> 200; case "COLLECTOR" -> 100; case "REVIEW" -> 40; case "COMMUNITY" -> 80; case "COMMUNICATOR" -> 200; case "ATTENDANCE" -> 100; default -> 9999; };
        if (level >= 20) return switch(type) { case "KNOWLEDGE" -> 70; case "COLLECTOR" -> 30; case "REVIEW" -> 15; case "COMMUNITY" -> 30; case "COMMUNICATOR" -> 70; case "ATTENDANCE" -> 30; default -> 9999; };
        if (level >= 10) return switch(type) { case "KNOWLEDGE" -> 30; case "COLLECTOR" -> 15; case "REVIEW" -> 7; case "COMMUNITY" -> 15; case "COMMUNICATOR" -> 30; case "ATTENDANCE" -> 15; default -> 9999; };
        if (level >= 5) return switch(type) { case "KNOWLEDGE" -> 10; case "COLLECTOR" -> 5; case "REVIEW" -> 3; case "COMMUNITY" -> 5; case "COMMUNICATOR" -> 10; case "ATTENDANCE" -> 7; default -> 9999; };
        return 1;
    }
}
