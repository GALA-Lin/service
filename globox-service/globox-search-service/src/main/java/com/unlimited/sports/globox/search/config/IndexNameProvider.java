package com.unlimited.sports.globox.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ES索引名称提供者
 * 根据不同环境生成带前缀的索引名称
 */
@Component
public class IndexNameProvider {

    @Value("${globox.env:}")
    private String env;

    /**
     * 统一搜索索引名
     */
    public String unifiedSearchIndex() {
        return buildIndexName("globox-unified-search");
    }

    /**
     * 笔记索引名
     */
    public String noteIndex() {
        return buildIndexName("globox-note");
    }

    /**
     * 场馆索引名
     */
    public String venueIndex() {
        return buildIndexName("globox-venue");
    }

    /**
     * 用户索引名
     */
    public String userIndex() {
        return buildIndexName("globox-user");
    }

    /**
     * 构建带环境前缀的索引名
     */
    private String buildIndexName(String baseName) {
        if (env == null || env.isEmpty()) {
            return baseName;
        }
        return env + "-" + baseName;
    }
}
