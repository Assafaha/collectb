package com.example.collectb.identity.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.collectb.identity.entity.RolePermission;

/** Spring Data repository for {@link RolePermission}. */
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

	List<RolePermission> findByRoleId(UUID roleId);

	boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

	void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

	boolean existsByPermissionId(UUID permissionId);
}
