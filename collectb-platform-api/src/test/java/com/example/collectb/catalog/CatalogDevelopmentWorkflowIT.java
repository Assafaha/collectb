package com.example.collectb.catalog;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "collectb.catalog.allow-development-mfa-bypass=true")
class CatalogDevelopmentWorkflowIT extends CatalogWorkflowIT {
}
