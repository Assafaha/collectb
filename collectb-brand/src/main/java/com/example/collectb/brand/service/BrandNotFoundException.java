package com.example.collectb.brand.service;

/** Thrown when a requested brand does not exist; mapped to HTTP 404 by {@code BrandController}. */
public class BrandNotFoundException extends RuntimeException {

	public BrandNotFoundException(String message) {
		super(message);
	}
}
