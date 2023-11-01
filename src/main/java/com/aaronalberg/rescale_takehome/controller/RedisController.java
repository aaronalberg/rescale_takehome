package com.aaronalberg.rescale_takehome.controller;

import com.aaronalberg.rescale_takehome.service.ProxyCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequiredArgsConstructor
public class RedisController {

    @Autowired
    private ProxyCacheService proxyCacheService;


    @GetMapping("/proxy/{key}")
    ResponseEntity<?> getValue(@PathVariable String key) throws JsonProcessingException {

        if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String cacheValue = proxyCacheService.getValue(key);

        if (cacheValue == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String,String> result = new HashMap<>();
        result.put("cacheValue", cacheValue);

        return ResponseEntity.ok(result);
    }

}
