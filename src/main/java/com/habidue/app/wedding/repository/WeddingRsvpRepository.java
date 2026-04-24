package com.habidue.app.wedding.repository;

import com.habidue.app.wedding.domain.WeddingRsvp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WeddingRsvpRepository extends JpaRepository<WeddingRsvp, Long> {

    List<WeddingRsvp> findByInvitationIdOrderByCreatedAtDesc(Long invitationId);

    @Query("SELECT COUNT(r) FROM WeddingRsvp r WHERE r.invitation.id = :id AND r.attendance = true")
    long countAttending(@Param("id") Long invitationId);

    @Query("SELECT COALESCE(SUM(r.guestCount), 0) FROM WeddingRsvp r WHERE r.invitation.id = :id AND r.attendance = true")
    long sumAttendingGuests(@Param("id") Long invitationId);
}
