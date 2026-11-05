package com.example.collectb.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.collectb.catalog.entity.Collection;

public interface CollectionRepository extends JpaRepository<Collection, UUID> {

	List<Collection> findByBrandIdOrderByCreatedAtDesc(UUID brandId);

	Optional<Collection> findByCollectionIdAndBrandId(UUID collectionId, UUID brandId);

	boolean existsByBrandIdAndCollectionCode(UUID brandId, String collectionCode);

	boolean existsByBrandIdAndCollectionCodeAndCollectionIdNot(UUID brandId, String collectionCode,
			UUID collectionId);
}
