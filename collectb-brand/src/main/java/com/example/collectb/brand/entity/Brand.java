package com.example.collectb.brand.entity;

import java.time.Instant;
import java.util.UUID;

import com.example.collectb.brand.model.BrandLicenseStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * JPA entity mapping the {@code brand} table (see
 * {@code db/changelog/changes/002-brand-catalog.xml}). A brand is the top of the
 * product-definition layer: it owns collections, which contain collectible templates. Brand
 * membership/role (who can administer this brand) is not modeled here - it is answered by the
 * Identity module's {@code role_assignment} table (scope_type=BRAND, scope_id=brand_id), so this
 * module only stores the raw {@code created_by_user_id} UUID, not a JPA association, matching
 * the "plain UUID foreign keys, no cross-module entity graphs" convention used throughout
 * collectb-identity.
 */
@Entity
@Table(name = "brand")
public class Brand {

	@Id
	@GeneratedValue
	@Column(name = "brand_id")
	private UUID brandId;

	@Column(name = "brand_code", nullable = false, unique = true)
	private String brandCode;

	@Column(name = "name", nullable = false, unique = true)
	private String name;

	@Column(name = "description")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "license_status", nullable = false)
	private BrandLicenseStatus licenseStatus;

	@Column(name = "created_by_user_id", nullable = false)
	private UUID createdByUserId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Brand() {
		// JPA
	}

	public Brand(String brandCode, String name, String description, UUID createdByUserId) {
		this.brandCode = brandCode;
		this.name = name;
		this.description = description;
		this.licenseStatus = BrandLicenseStatus.DRAFT;
		this.createdByUserId = createdByUserId;
	}

	@PrePersist
	protected void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public UUID getBrandId() {
		return brandId;
	}

	public String getBrandCode() {
		return brandCode;
	}

	public void setBrandCode(String brandCode) {
		this.brandCode = brandCode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BrandLicenseStatus getLicenseStatus() {
		return licenseStatus;
	}

	public void setLicenseStatus(BrandLicenseStatus licenseStatus) {
		this.licenseStatus = licenseStatus;
	}

	public UUID getCreatedByUserId() {
		return createdByUserId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
