package com.example.collectb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestcontainerConfiguration.class)
class CollectbApplicationTests {

	@Test
	void contextLoads() {
	}

}
