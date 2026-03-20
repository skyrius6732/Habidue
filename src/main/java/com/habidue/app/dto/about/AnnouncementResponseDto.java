package com.habidue.app.dto.about;

import com.habidue.app.domain.about.Announcement;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementResponseDto {
    private Long id;
    private String tag; // 이모지 포함 (전시용)
    private String rawTag; // 순수 텍스트 (수정용 매핑)
    private String type;
    private String date;
    private String title;
    private String content;

    public AnnouncementResponseDto(Announcement entity) {
        this.id = entity.getId();
        this.tag = ensureEmoji(entity.getTag());
        this.rawTag = entity.getTag(); // 원본 텍스트 그대로 저장
        this.type = entity.getType();
        this.date = entity.getDate();
        this.title = entity.getTitle();
        this.content = entity.getContent();
    }

    private String ensureEmoji(String tag) {
        if (tag == null) return null;
        if (tag.contains("📌") || tag.contains("📢") || tag.contains("📝") || tag.contains("🎁") || 
            tag.contains("⚙️") || tag.contains("🚨") || tag.contains("🆕")) {
            return tag;
        }
        
        // 이모지가 없는 경우 텍스트 매칭으로 추가
        switch (tag) {
            case "중요": return "📌 중요";
            case "안내": return "📢 안내";
            case "공지": return "📝 공지";
            case "이벤트": return "🎁 이벤트";
            case "점검": return "⚙️ 점검";
            case "긴급": return "🚨 긴급";
            case "업데이트": return "🆕 업데이트";
            default: return tag;
        }
    }
}
