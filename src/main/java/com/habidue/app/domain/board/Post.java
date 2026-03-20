package com.habidue.app.domain.board;

import com.habidue.app.domain.common.BaseTimeEntity;
import com.habidue.app.domain.user.User;
import com.habidue.app.domain.notice.Notice;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_post_status_created", columnList = "status, createdAt"),
    @Index(name = "idx_post_author", columnList = "user_id"),
    @Index(name = "idx_post_notice", columnList = "notice_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id")
    private Notice notice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostType type;

    @Column
    private String category;

    @Column
    private String subCategory;

    @Column
    private String regionTag; 

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<PostImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private List<PostTag> tags = new ArrayList<>();

    // --- 연관관계 편의 메서드 ---
    public void addPostTag(PostTag postTag) {
        this.tags.add(postTag);
        if (postTag.getPost() != this) {
            postTag.setPost(this);
        }
    }

    public void clearTags() {
        this.tags.clear();
    }

    // --- 비즈니스 로직 ---
    public void changeStatus(String status) {
        this.status = status;
    }

    public void update(String title, String content, String category, String subCategory, String regionTag) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.subCategory = subCategory;
        this.regionTag = regionTag;
    }
}
