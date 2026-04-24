package com.habidue.app.wedding.repository;

import com.habidue.app.wedding.domain.WeddingGuestbook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeddingGuestbookRepository extends JpaRepository<WeddingGuestbook, Long> {

    List<WeddingGuestbook> findByInvitationIdAndHiddenFalseOrderByCreatedAtDesc(Long invitationId);

    List<WeddingGuestbook> findByInvitationIdOrderByCreatedAtDesc(Long invitationId);
}
