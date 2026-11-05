package com.example.collectb.brand.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.collectb.brand.entity.Brand;

/** Spring Data repository for {@link Brand}. */
public interface BrandRepository extends JpaRepository<Brand, UUID> {

	boolean existsByBrandCode(String brandCode);

	boolean existsByName(String name);
}
