package com.habidue.app.wedding.repository;

import com.habidue.app.wedding.domain.WeddingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeddingPhotoRepository extends JpaRepository<WeddingPhoto, Long> {

    List<WeddingPhoto> findByInvitationIdOrderByOrderNumAsc(Long invitationId);

    void deleteByInvitationId(Long invitationId);
}
