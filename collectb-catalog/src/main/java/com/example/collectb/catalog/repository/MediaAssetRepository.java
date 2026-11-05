package com.example.collectb.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.collectb.catalog.entity.MediaAsset;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

	List<MediaAsset> findByBrandIdOrderByCreatedAtDesc(UUID brandId);

	Optional<MediaAsset> findByMediaAssetIdAndBrandId(UUID mediaAssetId, UUID brandId);

	boolean existsByBrandIdAndStorageUri(UUID brandId, String storageUri);

	boolean existsByMediaAssetIdAndBrandId(UUID mediaAssetId, UUID brandId);
}
