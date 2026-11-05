package com.example.collectb.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.collectb.identity.entity.Permission;

/** Spring Data repository for {@link Permission}. */
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

	/** Resolves the distinct set of permission keys granted (via role_permission) to any of the
	 *  given role ids - used when building JWT/authority claims for an authenticated user. */
	@Query("select distinct p.permissionKey from Permission p, RolePermission rp "
			+ "where rp.permissionId = p.permissionId and rp.roleId in :roleIds")
	List<String> findPermissionKeysByRoleIdIn(@Param("roleIds") List<UUID> roleIds);

	Optional<Permission> findByPermissionKey(String permissionKey);
}
