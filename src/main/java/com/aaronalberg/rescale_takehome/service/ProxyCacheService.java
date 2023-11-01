package com.aaronalberg.rescale_takehome.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.TimeUnit;

@Service
public class ProxyCacheService {

    private final LoadingCache<String, String> cache;

    @Autowired
    public ProxyCacheService(Environment env) {
        int cacheSize = Integer.parseInt(env.getRequiredProperty("cacheSize"));
        int localCacheExpirySeconds = Integer.parseInt(env.getRequiredProperty("localCacheExpirySeconds"));

        cache = CacheBuilder.newBuilder()
                .maximumSize(cacheSize)
                .expireAfterWrite(localCacheExpirySeconds, TimeUnit.SECONDS)
                .build(
                        new CacheLoader<>() {
                            @Override
                            public String load(String key) {
                                JedisPool pool = new JedisPool("localhost", 6379);

                                try (Jedis jedis = pool.getResource()) {
                                    return jedis.get(key);
                                }
                            }
                        });
    }

    public String getValue(String key) {
        try {
            return cache.get(key);
        } catch (Exception e) {
            return null;
        }
    }
}
