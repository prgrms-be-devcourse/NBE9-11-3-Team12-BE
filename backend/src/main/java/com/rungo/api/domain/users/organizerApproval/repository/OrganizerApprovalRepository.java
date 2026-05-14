package com.rungo.api.domain.users.organizerApproval.repository;

import com.rungo.api.domain.users.organizerApproval.entity.OrganizerApproval;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizerApprovalRepository extends JpaRepository<OrganizerApproval,Long> {
}
