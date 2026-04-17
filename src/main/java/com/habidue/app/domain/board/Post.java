package com.habidue.app.domain.board;

import com.habidue.app.domain.barter.BarterStatus;
import com.habidue.app.domain.barter.ItemCondition;
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
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
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

    // --- 물물교환 전용 필드 ---
    @Column
    private String itemName;                        // 내 물건 품명 (사용자 작성)

    @Column
    private String wantedItem;

    @Enumerated(EnumType.STRING)
    @Column
    private ItemCondition itemCondition;

    @Enumerated(EnumType.STRING)
    @Column
    private BarterStatus barterStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_method")
    private com.habidue.app.domain.barter.TradeMethod preferredMethod;

    @Column(name = "preferred_date")
    private java.time.LocalDate preferredDate;

    @Column(name = "preferred_time")
    private String preferredTime;
    // ------------------------

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
    @Builder.Default
    private List<PostLike> postLikes = new ArrayList<>();

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

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

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

    public void update(String title, String content, PostType type, String category, String subCategory, String regionTag) {
        this.title = title;
        this.content = content;
        this.type = type;
        this.category = category;
        this.subCategory = subCategory;
        this.regionTag = regionTag;
    }

    public void updateBarter(String itemName, String wantedItem, ItemCondition itemCondition, BarterStatus barterStatus,
                             com.habidue.app.domain.barter.TradeMethod preferredMethod,
                             java.time.LocalDate preferredDate, String preferredTime) {
        this.itemName = itemName;
        this.wantedItem = wantedItem;
        this.itemCondition = itemCondition;
        this.barterStatus = barterStatus;
        this.preferredMethod = preferredMethod;
        this.preferredDate = preferredDate;
        this.preferredTime = preferredTime;
    }

    public void changeStatus(String status) {
        this.status = status;
    }
}
