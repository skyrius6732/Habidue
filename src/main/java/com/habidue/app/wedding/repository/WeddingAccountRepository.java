package com.habidue.app.wedding.repository;

import com.habidue.app.wedding.domain.WeddingAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeddingAccountRepository extends JpaRepository<WeddingAccount, Long> {

    List<WeddingAccount> findByInvitationId(Long invitationId);

    void deleteByInvitationId(Long invitationId);
}
