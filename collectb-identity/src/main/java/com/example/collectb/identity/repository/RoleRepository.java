package com.example.collectb.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.collectb.identity.entity.Role;

/** Spring Data repository for {@link Role}. */
public interface RoleRepository extends JpaRepository<Role, UUID> {

	Optional<Role> findByRoleName(String roleName);

	List<Role> findByRoleIdIn(List<UUID> roleIds);

	List<Role> findByRoleType(String roleType);
}
