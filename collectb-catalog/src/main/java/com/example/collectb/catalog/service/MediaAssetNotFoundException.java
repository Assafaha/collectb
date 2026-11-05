package com.example.collectb.catalog.service;

public class MediaAssetNotFoundException extends RuntimeException {

	public MediaAssetNotFoundException(String message) {
		super(message);
	}
}
