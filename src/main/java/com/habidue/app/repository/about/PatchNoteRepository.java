package com.habidue.app.repository.about;

import com.habidue.app.domain.about.PatchNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatchNoteRepository extends JpaRepository<PatchNote, Long> {
    @Query("SELECT DISTINCT p FROM PatchNote p LEFT JOIN FETCH p.details ORDER BY p.createdAt DESC")
    List<PatchNote> findAllWithDetailsOrderByCreatedAtDesc();
}
