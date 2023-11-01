# Aaron Alberg Rescale Takehome

## Background
This project implements an HTTP proxy for [Redis](insert link) as requested by Rescale

## High-level architecture overview.

The application has 3 distinct sections:
- Config: where services for connecting to the backing redis instance are configured so that they can
be accessed in any service (and then work out the box with Spring's `@Autowired` annotation). See 'What the code does' for details on configuring application constants.
- Controller: where REST requests are received and then delegated to the proper service, 
handling only parsing input and preparing response
- Service: where the *real* logic of the application lives for the caching operations

## What the code does.

When a valid request is received, the in-memory cache will be checked first. If the in-memory cache doesn't
have a valid entry for the key, the backing redis instance is queried for the key. 
If there is a value in the backing redis instance, that value is stored in the in-memory cache with the globally
configured Time-to-live (TTL). 

The in-memory cache is implemented with Google's Guava library. The `LoadingCache` of this library is ideal because
it supports configuration including global expiry, a maximum number of keys, and uses LRU eviction policy by default
[(source)](https://guava.dev/releases/19.0/api/docs/com/google/common/cache/CacheBuilder.html#:~:text=least%2Drecently%2Dused%20eviction%20when%20a%20maximum%20size%20is%20exceeded).
It also a simple API for providing the ability to define behavior for in-memory cache misses.

#### Features supported:


- **Application Configuration**: the following fields can be configured before application startup 
  in `src/main/resources/application.properties`
  - localCacheExpirySeconds - amount of time before a local cache entry is invalid. A request will be sent to backing 
  redis to lookup the value after this amount of time has passed.
  - redisCacheExpirySeconds - amount of time before backing redis will consider the entry invalid. A request for an expired entry will result in
  a not found result.
  - cacheSize - the maximum number of keys in the local cache
  - redisHostName - the host where the application should look to connect to backing redis
  - redisPort - same as above, but the port
  - server.tomcat.threads.max - maximum number of threads that will be created to serve HTTP requests
  - server.tomcat.accept-count - maximum number of threads will be created to hold incoming requests when the threads serving requests are all busy
- **Container Configuration**: the listening ports for the application and backing redis can be
    configured in `docker-compose.yml`
- **Cached GET**: the in-memory cache is searched before making a request to redis
- **Global Expiry**: values added to the cache are expired after a time duration (see above config section)
- **Fixed Key Size**: only a set number of keys are present in the cache (see above config section)
- **Sequential Concurrent Processing**: multiple clients can submit requests to the proxy. This is supported by Spring by default
- **Parallel Concurrent Processing**: multiple requests can be served in parallel. This is supported by Spring by default.
- **Concurrent Client Limit**: the number of clients that can be served in parallel can be configured (see above config section)


## Algorithmic complexity of the cache operations.

#### Space complexity
The in-memory cache theoretically requires `O(n)` space where `n` is the number of cache entries that
haven't expired. However, since this application uses a fixed number for the max limit of keys, the upper constraint is
instead set by this value. Similarly, the Redis instance will evict members when memory is full, so the amount
of available memory on the machine will be the upper bound.

#### Time complexity
Both the in-memory cache and backing Redis support `O(1)` constant time operations for 
retrieving cache value operations (`GET`). Though not directly supported by this application, `SET`/`PUT` 
operations are also able to be completed in `O(1)` amortized time.

## Instructions for how to run the proxy and tests.
To run all the tests:
```
make test
```

To run the proxy locally:
```
make run
```

Both of these commands are really just wrappers for docker build and run commands.

## How long you spent on each part of the project.
- Reading assignment, reading up on Redis and associated technologies: 30 min
- Setting up Spring application and connecting backing redis, making sure everything works: 60 min
- Implementing caching logic: 60 min
- Writing tests: 45 min
- Configuring Docker commands, Makefile, testing environment: 45 min
- Writing this README: 30 min

Total: 4.5 hours

## A list of the requirements that you did not implement and the reasons for omitting them.

The Redis client protocol bonus requirement is not supported because of its complexity given the scope of this assignment.




## Sources

- [Spring initializr](https://start.spring.io/) for generic files for a Spring Boot web application
- [Spring docs](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/application-properties.html#application-properties.server.server.tomcat.threads.max)
    for details on Spring's default properties, behaviors, customization
- [Redis](https://redis.io/commands/get/) for basic understanding of Redis API as well as time complexities of operations
- [Jedis](https://github.com/redis/jedis) Java client for Redis recommended by Redis
- [Google's Guava Cache](https://github.com/google/guava/wiki/CachesExplained) for simple in memory cache
- [Docker guides](https://docs.docker.com/language/java/build-images/) for 
guides on docker commands, docker compose, etc
- Various stack overflow posts for help with getting the above libraries versions working properly
and connecting to the backing redis instance