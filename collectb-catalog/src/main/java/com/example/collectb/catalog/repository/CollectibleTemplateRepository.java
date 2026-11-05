package com.example.collectb.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.collectb.catalog.entity.CollectibleTemplate;
import com.example.collectb.catalog.model.TemplateStatus;

public interface CollectibleTemplateRepository extends JpaRepository<CollectibleTemplate, UUID> {

	List<CollectibleTemplate> findByCollectionIdOrderByCreatedAtDesc(UUID collectionId);

	Optional<CollectibleTemplate> findByTemplateIdAndCollectionId(UUID templateId, UUID collectionId);

	boolean existsByCollectionIdAndTemplateCode(UUID collectionId, String templateCode);

	boolean existsByCollectionIdAndTemplateCodeAndTemplateIdNot(UUID collectionId, String templateCode,
			UUID templateId);

	boolean existsByCollectionId(UUID collectionId);

	boolean existsByCollectionIdAndPublishStatus(UUID collectionId, TemplateStatus publishStatus);
}
