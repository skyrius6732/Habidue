package com.habidue.app.domain.about;

import com.habidue.app.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "patch_notes")
public class PatchNote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false)
    private String title; // 패치노트 제목 추가

    @Column(length = 20)
    private String date; // 표시용 날짜

    @OneToMany(mappedBy = "patchNote", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC") // 상세 내역 순서 보장
    @Builder.Default
    private List<PatchNoteDetail> details = new ArrayList<>();

    public void update(String title, String version, String date) {
        this.title = title;
        this.version = version;
        this.date = date;
    }
}
