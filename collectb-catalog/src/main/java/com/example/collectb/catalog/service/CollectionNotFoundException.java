package com.example.collectb.catalog.service;

public class CollectionNotFoundException extends RuntimeException {

	public CollectionNotFoundException(String message) {
		super(message);
	}
}
