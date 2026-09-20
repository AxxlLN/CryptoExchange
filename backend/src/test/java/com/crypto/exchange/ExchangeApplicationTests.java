package com.crypto.exchange;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class ExchangeApplicationTests {

	@Test
	void contextLoads() {
		// Коммент чтобы сонар не ругался
	}

}
