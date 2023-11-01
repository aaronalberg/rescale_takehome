package com.aaronalberg.rescale_takehome;

import com.aaronalberg.rescale_takehome.controller.RedisController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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

		try (Jedis jedis = pool.getResource()) {
			jedis.set("key", "value");
		}


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
	}

	@Test
	void keyNotPresent() {
		assertThat(redisController).isNotNull();
		assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/proxy/randomkey",
				String.class)).contains("KEY NOT PRESENT");
	}

	@Test
	void useLocalCache() {
		try (Jedis jedis = pool.getResource()) {
			jedis.set("KEY", "VALUE1");
		}

		assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/proxy/KEY",
				String.class)).contains("VALUE1");

		try (Jedis jedis = pool.getResource()) {
			jedis.set("KEY", "VALUE2");
		}

		assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/proxy/KEY",
				String.class)).contains("VALUE1");
	}

	@Test
	void localCacheExpired() throws InterruptedException {
		try (Jedis jedis = pool.getResource()) {
			jedis.set("KEY", "VALUE1");
		}

		assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/proxy/KEY",
				String.class)).contains("VALUE1");

		try (Jedis jedis = pool.getResource()) {
			jedis.set("KEY", "VALUE2");
		}

		// note this may fail if the local cache expiry time is changed
		Thread.sleep(5000);
		assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/proxy/KEY",
				String.class)).contains("VALUE2");
	}
}
