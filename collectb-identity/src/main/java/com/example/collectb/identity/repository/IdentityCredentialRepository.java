package com.example.collectb.identity.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.collectb.identity.entity.IdentityCredential;
import com.example.collectb.identity.model.CredentialType;

/** Spring Data repository for {@link IdentityCredential}. */
public interface IdentityCredentialRepository extends JpaRepository<IdentityCredential, UUID> {

	List<IdentityCredential> findByUserId(UUID userId);

	List<IdentityCredential> findByUserIdAndCredentialType(UUID userId, CredentialType credentialType);
}
