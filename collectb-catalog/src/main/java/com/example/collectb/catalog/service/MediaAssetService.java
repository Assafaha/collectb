package com.example.collectb.catalog.service;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.collectb.catalog.entity.MediaAsset;
import com.example.collectb.catalog.repository.MediaAssetRepository;
import com.example.collectb.catalog.web.CreateMediaAssetRequest;

@Service
public class MediaAssetService {

	private static final String BRAND_SCOPE = "@brandScopeAuthorizationService.hasBrandRole(authentication, "
			+ "#brandId, 'BRAND_ADMIN', 'BRAND_EDITOR')";

	private final MediaAssetRepository mediaAssetRepository;
	private final BrandCatalogPolicy brandCatalogPolicy;

	public MediaAssetService(MediaAssetRepository mediaAssetRepository, BrandCatalogPolicy brandCatalogPolicy) {
		this.mediaAssetRepository = mediaAssetRepository;
		this.brandCatalogPolicy = brandCatalogPolicy;
	}

	@PreAuthorize("hasAuthority('MEDIA_ASSET:READ') and "
			+ "@brandScopeAuthorizationService.hasBrandRole(authentication, #brandId, "
			+ "'BRAND_ADMIN', 'BRAND_EDITOR', 'BRAND_VIEWER')")
	@Transactional(readOnly = true)
	public List<MediaAsset> list(UUID brandId) {
		return mediaAssetRepository.findByBrandIdOrderByCreatedAtDesc(brandId);
	}

	@PreAuthorize("hasAuthority('MEDIA_ASSET:UPLOAD') and " + BRAND_SCOPE)
	@Transactional
	public MediaAsset register(UUID brandId, CreateMediaAssetRequest request, Authentication authentication) {
		brandCatalogPolicy.requireActiveBrand(brandId);
		String assetType = required(request.assetType(), "assetType", 30);
		String storageUri = validateUri(request.storageUri(), "storageUri", true);
		String cdnUri = request.cdnUri() == null || request.cdnUri().isBlank()
				? null : validateUri(request.cdnUri(), "cdnUri", false);
		String checksum = required(request.checksum(), "checksum", 128).toLowerCase(Locale.ROOT);
		if (!checksum.matches("[0-9a-f]{64}|[0-9a-f]{128}")) {
			throw new IllegalArgumentException("checksum must be a SHA-256 or SHA-512 hex digest");
		}
		if (request.fileSizeBytes() != null && request.fileSizeBytes() <= 0) {
			throw new IllegalArgumentException("fileSizeBytes must be positive");
		}
		if (request.contentType() != null && request.contentType().length() > 100) {
			throw new IllegalArgumentException("contentType must not exceed 100 characters");
		}
		if (mediaAssetRepository.existsByBrandIdAndStorageUri(brandId, storageUri)) {
			throw new IllegalArgumentException("A media asset with this storageUri is already registered");
		}
		return mediaAssetRepository.save(new MediaAsset(brandId, assetType, storageUri, cdnUri,
				request.contentType(), checksum, request.fileSizeBytes(), CatalogActor.userId(authentication)));
	}

	@Transactional(readOnly = true)
	public MediaAsset requireForBrand(UUID mediaAssetId, UUID brandId) {
		return mediaAssetRepository.findByMediaAssetIdAndBrandId(mediaAssetId, brandId)
				.orElseThrow(() -> new MediaAssetNotFoundException(
						"No media asset " + mediaAssetId + " for brand " + brandId));
	}

	private static String validateUri(String value, String field, boolean allowS3) {
		String normalized = required(value, field, 2048);
		try {
			URI uri = URI.create(normalized);
			String scheme = uri.getScheme();
			boolean valid = scheme != null && (scheme.equalsIgnoreCase("https")
					|| allowS3 && scheme.equalsIgnoreCase("s3"));
			if (!valid || scheme.equalsIgnoreCase("https") && uri.getHost() == null) {
				throw new IllegalArgumentException(field + " must be an absolute HTTPS"
						+ (allowS3 ? " or S3" : "") + " URI");
			}
			return uri.toString();
		} catch (IllegalArgumentException ex) {
			if (ex.getMessage() != null && ex.getMessage().startsWith(field + " ")) {
				throw ex;
			}
			throw new IllegalArgumentException(field + " must be a valid absolute URI", ex);
		}
	}

	private static String required(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " is required");
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
		}
		return normalized;
	}
}
