package com.example.collectb.identity.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.collectb.identity.entity.PlatformUser;

/** Spring Data repository for {@link PlatformUser}. */
public interface PlatformUserRepository extends JpaRepository<PlatformUser, UUID> {

	Optional<PlatformUser> findByEmail(String email);
}
