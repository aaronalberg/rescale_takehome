package com.aaronalberg.rescale_takehome;

import com.aaronalberg.rescale_takehome.controller.RedisController;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.embedded.RedisServer;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RescaleTakehomeApplicationTests {

	@Autowired
	RedisController redisController;

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;


	private JedisPool pool;
	private RedisServer redisServer;

	@BeforeAll
	void setup() throws IOException {
		redisServer = new RedisServer(6379);
		redisServer.start();

		pool = new JedisPool("localhost", 6379);
	}

	@AfterAll
	void tearDown() {
		try (Jedis jedis = pool.getResource()) {
			jedis.flushDB();
		}

		redisServer.stop();
	}

	@Test
	void contextLoads() {
		assertThat(redisController).isNotNull();
	}

	@Test
	void keyNotPresent() {
		assertThat(get("randomkey")).isNullOrEmpty();
	}

	@Test
	void normalGet() {
		String key = "KEY1";
		String val = "VALUE1";

		setCache(key, val);

		assertThat(get(key)).contains(val);

	}

	@Test
	void useLocalCacheBeforeExpiry() {
		String key = "key2";
		String val1 = "BEFOREVAL";
		String val2 = "AFTERVAL";

		setCache(key, val1);
		assertThat(get(key)).contains(val1);

		setCache(key, val2);
		assertThat(get(key)).contains(val1);
	}

	@Test
	void useRedisAfterExpiry() throws InterruptedException {
		String key = "key3";
		String val1 = "BEFOREVAL";
		String val2 = "AFTERVAL";

		setCache(key, val1);
		assertThat(get(key)).contains(val1);

		setCache(key, val2);

		// note this may fail if the local cache expiry time is changed
		Thread.sleep(5000);
		assertThat(get(key)).contains(val2);
	}

	@Test
	void handleMultipleKeys() {
		String key1 = "abc";
		String val1 = "val1";
		String key2 = "def";
		String val2 = "val2";
		String key3 = "ghi";
		String val3 = "val3";

		setCache(key1, val1);
		setCache(key2, val2);
		setCache(key3, val3);

		assertThat(get(key1)).contains(val1);
		assertThat(get(key2)).contains(val2);
		assertThat(get(key3)).contains(val3);
	}

	private String get(String key) {
		return this.restTemplate.getForObject("http://localhost:" + port + "/proxy/" + key,
				String.class);
	}

	private void setCache(String key, String val) {
		try (Jedis jedis = pool.getResource()) {
			jedis.set(key, val);
		}
	}
}
