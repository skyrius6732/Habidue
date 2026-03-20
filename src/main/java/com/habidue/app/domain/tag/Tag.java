package com.habidue.app.domain.tag;

import com.habidue.app.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * 태그 엔티티 - 이름과 타입의 조합으로 유일성을 가짐
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@BatchSize(size = 100)
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_tag_name_type", columnNames = {"name", "type"})
})
public class Tag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TagType type;

    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<com.habidue.app.domain.tag.NoticeTag> noticeTags = new ArrayList<>();

    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<com.habidue.app.domain.board.PostTag> postTags = new ArrayList<>();

    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserTag> userTags = new ArrayList<>();

    @Builder
    public Tag(String name, TagType type) {
        this.name = name;
        this.type = type;
    }

    /**
     * 태그 정보 업데이트 메서드
     */
    public void update(String name, TagType type) {
        this.name = name;
        this.type = type;
    }
}
