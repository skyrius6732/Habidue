package com.habidue.app.wedding.repository;

import com.habidue.app.wedding.domain.WeddingInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WeddingInvitationRepository extends JpaRepository<WeddingInvitation, Long> {

    Optional<WeddingInvitation> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<WeddingInvitation> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE WeddingInvitation w SET w.viewCount = w.viewCount + 1 WHERE w.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
