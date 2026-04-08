-- ============================================================
-- 더미 데이터 삭제 스크립트
-- init_posts_dummy_data.sql 에서 추가한 모든 데이터를 삭제합니다
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 더미 사용자들의 게시글 관련 데이터 삭제
DELETE FROM post_likes WHERE post_id IN (
    SELECT id FROM posts WHERE user_id IN (
        SELECT id FROM users WHERE username IN (
            'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
            'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
            'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
            'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
        )
    )
);

DELETE FROM post_tags WHERE post_id IN (
    SELECT id FROM posts WHERE user_id IN (
        SELECT id FROM users WHERE username IN (
            'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
            'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
            'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
            'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
        )
    )
);

DELETE FROM post_images WHERE post_id IN (
    SELECT id FROM posts WHERE user_id IN (
        SELECT id FROM users WHERE username IN (
            'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
            'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
            'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
            'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
        )
    )
);

DELETE FROM comment_likes WHERE comment_id IN (
    SELECT id FROM comments WHERE post_id IN (
        SELECT id FROM posts WHERE user_id IN (
            SELECT id FROM users WHERE username IN (
                'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
                'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
                'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
                'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
            )
        )
    )
);

DELETE FROM comments WHERE post_id IN (
    SELECT id FROM posts WHERE user_id IN (
        SELECT id FROM users WHERE username IN (
            'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
            'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
            'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
            'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
        )
    )
);

-- 2. 더미 사용자들의 게시글 삭제
DELETE FROM posts WHERE user_id IN (
    SELECT id FROM users WHERE username IN (
        'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
        'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
        'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
        'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
    )
);

-- 3. 더미 사용자들이 작성한 댓글 삭제 (다른 사용자 게시글에 단 댓글)
DELETE FROM comment_likes WHERE comment_id IN (
    SELECT id FROM comments WHERE user_id IN (
        SELECT id FROM users WHERE username IN (
            'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
            'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
            'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
            'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
        )
    )
);

DELETE FROM comments WHERE user_id IN (
    SELECT id FROM users WHERE username IN (
        'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
        'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
        'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
        'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
    )
);

-- 4. 더미 사용자들이 한 좋아요 삭제
DELETE FROM post_likes WHERE user_id IN (
    SELECT id FROM users WHERE username IN (
        'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
        'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
        'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
        'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
    )
);

DELETE FROM comment_likes WHERE user_id IN (
    SELECT id FROM users WHERE username IN (
        'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
        'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
        'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
        'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
    )
);

-- 5. 더미 사용자들의 다른 데이터 삭제 (cascade가 설정되어 있으면 자동 삭제되지만, 명시적으로)
DELETE FROM user_badges WHERE user_id IN (
    SELECT id FROM users WHERE username IN (
        'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
        'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
        'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
        'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
    )
);

DELETE FROM user_notices WHERE user_id IN (
    SELECT id FROM users WHERE username IN (
        'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
        'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
        'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
        'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
    )
);

DELETE FROM user_tag WHERE user_id IN (
    SELECT id FROM users WHERE username IN (
        'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
        'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
        'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
        'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
    )
);

DELETE FROM user_blocks WHERE blocker_id IN (
    SELECT id FROM users WHERE username IN (
        'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
        'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
        'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
        'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
    )
) OR blocked_id IN (
    SELECT id FROM users WHERE username IN (
        'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
        'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
        'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
        'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
    )
);

DELETE FROM user_activity_stats WHERE user_id IN (
    SELECT id FROM users WHERE username IN (
        'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
        'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
        'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
        'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
    )
);

DELETE FROM attendance WHERE user_id IN (
    SELECT id FROM users WHERE username IN (
        'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
        'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
        'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
        'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
    )
);

DELETE FROM exp_history WHERE user_id IN (
    SELECT id FROM users WHERE username IN (
        'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
        'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
        'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
        'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
    )
);

DELETE FROM karma_history WHERE user_id IN (
    SELECT id FROM users WHERE username IN (
        'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
        'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
        'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
        'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
    )
);

-- 6. 더미 사용자들 삭제 (마지막)
DELETE FROM users WHERE username IN (
    'test_01', 'test_02', 'test_03', 'test_04', 'test_05',
    'test_06', 'test_07', 'test_08', 'test_09', 'test_10',
    'test_11', 'test_12', 'test_13', 'test_14', 'test_15',
    'test_16', 'test_17', 'test_18', 'test_19', 'test_20'
);

SET FOREIGN_KEY_CHECKS = 1;

COMMIT;
