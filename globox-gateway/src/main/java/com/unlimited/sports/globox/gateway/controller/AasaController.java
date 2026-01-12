package com.unlimited.sports.globox.gateway.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * aasa 资源获取 for apple Universal Links
 */
@RestController
public class AasaController {

    private final ResourceLoader resourceLoader;

    public AasaController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @GetMapping(
            value = {"/apple-app-site-association", "/.well-known/apple-app-site-association"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<String>> aasa() {
        Resource resource = resourceLoader.getResource("classpath:aasa/apple-app-site-association");

        return Mono.fromCallable(() -> {
                    try (InputStream is = resource.getInputStream()) {
                        return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
                    }
                })
                // 避免在 Netty event-loop 线程里做阻塞 IO
                .subscribeOn(Schedulers.boundedElastic())
                .map(json -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        // 避免浏览器把它当附件下载（可选）
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        // 联调阶段建议 no-store（上线可按需调整）
                        .cacheControl(CacheControl.noStore())
                        .body(json)
                );
    }
}