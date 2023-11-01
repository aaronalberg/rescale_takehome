package com.aaronalberg.rescale_takehome.controller;

import com.aaronalberg.rescale_takehome.service.ProxyCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequiredArgsConstructor
public class RedisController {

    @Autowired
    private ProxyCacheService proxyCacheService;

    @GetMapping("/proxy/{key}")
    ResponseEntity<?> getValue(@PathVariable String key) {

        String res = proxyCacheService.getValue(key);

        if (res == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("KEY NOT PRESENT IN CACHE");
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(res);
    }
}
