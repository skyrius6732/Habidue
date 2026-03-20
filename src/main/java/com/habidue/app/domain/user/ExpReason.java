package com.habidue.app.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExpReason {
    POST_CREATED("게시글 작성", 10, ExpCategory.SINCERITY),
    COMMENT_CREATED("댓글 작성", 3, ExpCategory.SINCERITY),
    RECEIVED_LIKE("좋아요 받음", 5, ExpCategory.SINCERITY),
    KNOWLEDGE_LIKE("지식글 좋아요 수신", 5, ExpCategory.KNOWLEDGE),
    REVIEW_LIKE("리뷰글 좋아요 수신", 10, ExpCategory.REVIEW),
    ATTENDANCE("연속 출석", 20, ExpCategory.SINCERITY),
    BADGE_ACQUIRED("칭호 획득 보너스", 50, ExpCategory.SINCERITY),
    ADMIN_BONUS("관리자 보너스", 0, ExpCategory.ADMIN),
    
    // [시니어] 지식/리뷰 활동 세분화
    KNOWLEDGE_SHARE("지식 공유글 작성", 30, ExpCategory.KNOWLEDGE),
    QUESTION_ANSWERED("질문 답변 채택", 50, ExpCategory.KNOWLEDGE),
    PRODUCT_REVIEW("상품 리뷰 작성", 40, ExpCategory.REVIEW),
    LOCATION_REVIEW("장소 리뷰 작성", 40, ExpCategory.REVIEW),
    KNOWLEDGE_PHOTO_BONUS("지식글 사진 보너스", 10, ExpCategory.KNOWLEDGE),
    REVIEW_PHOTO_BONUS("리뷰글 사진 보너스", 10, ExpCategory.REVIEW),
    SINCERITY_PHOTO_BONUS("일반글 사진 보너스", 10, ExpCategory.SINCERITY);

    private final String description;
    private final int defaultExp; // 기본 부여 경험치량
    private final ExpCategory category;
}