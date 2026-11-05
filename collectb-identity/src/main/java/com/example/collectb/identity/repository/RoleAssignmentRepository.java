package com.example.collectb.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.collectb.identity.entity.RoleAssignment;

/** Spring Data repository for {@link RoleAssignment}. */
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {

	List<RoleAssignment> findByUserIdAndStatus(UUID userId, String status);

	Optional<RoleAssignment> findByUserIdAndRoleIdAndScopeTypeAndScopeId(UUID userId, UUID roleId, String scopeType,
			UUID scopeId);

	List<RoleAssignment> findByScopeTypeAndScopeIdAndStatus(String scopeType, UUID scopeId, String status);

	List<RoleAssignment> findByScopeTypeAndStatus(String scopeType, String status);

	List<RoleAssignment> findByStatus(String status);

	List<RoleAssignment> findByScopeTypeAndScopeIdInAndStatus(String scopeType, List<UUID> scopeIds, String status);

	List<RoleAssignment> findByUserIdAndScopeTypeAndStatus(UUID userId, String scopeType, String status);
}
