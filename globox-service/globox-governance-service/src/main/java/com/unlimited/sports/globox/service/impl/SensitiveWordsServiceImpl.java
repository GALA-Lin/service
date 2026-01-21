package com.unlimited.sports.globox.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.COSClient;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.prop.CosProperties;
import com.unlimited.sports.globox.common.result.GovernanceCode;
import com.unlimited.sports.globox.cos.CosFileDownloadUtils;
import com.unlimited.sports.globox.cos.dto.BatchDownloadResultDto;
import com.unlimited.sports.globox.holder.SensitiveWordsHolder;
import com.unlimited.sports.globox.prop.SensitiveManifest;
import com.unlimited.sports.globox.service.SensitiveWordsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import toolgood.words.WordsSearchEx;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 敏感词 service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordsServiceImpl implements SensitiveWordsService {
    private final COSClient cosClient;
    private final CosProperties cosProperties;
    private final ObjectMapper objectMapper;
    private final SensitiveWordsHolder holder;

    @Value("${cos.sensitive-words.base-prefix}")
    private String basePrefix;

    @Value("${cos.sensitive-words.manifest}")
    private String manifestKey;


    @Override
    public void reload() {
        try {

            // 1) 下载 manifest.json（注意：这里必须是 manifestKey，不是 basePrefix）
            String manifestJson = CosFileDownloadUtils.downloadFromCosToString(
                    cosClient, cosProperties, basePrefix + '/' + manifestKey
            );
            if (manifestJson == null || manifestJson.isBlank()) {
                log.error("manifest is empty: {}", manifestKey);
                throw new GloboxApplicationException(GovernanceCode.MANIFEST_EMPTY);
            }

            SensitiveManifest manifest = objectMapper.readValue(manifestJson, SensitiveManifest.class);
            if (manifest.getFiles() == null || manifest.getFiles().isEmpty()) {
                log.error("manifest.files is empty, version={}", manifest.getVersion());
                throw new GloboxApplicationException(GovernanceCode.MANIFEST_EMPTY);
            }

            // 2) 组装对象 key 列表（key 用原始文件名，不要 URL encode）
            List<String> keys = manifest.getFiles().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(name -> basePrefix + '/' + name)
                    .collect(Collectors.toList());

            // 3) 批量下载
            BatchDownloadResultDto r = CosFileDownloadUtils.batchDownloadFromCosToString(
                    cosClient, cosProperties, keys,
                    8,
                    false
            );

            if (!r.errors().isEmpty()) {
                log.error("Sensitive words download failed: {}", r.errors().keySet());
                throw new GloboxApplicationException(GovernanceCode.SENSITIVE_WORDS_DOWNLOAD_FAILED);
            }

            // 4) 合并去重：逐行抽词
            Set<String> keywords = new LinkedHashSet<>();
            for (Map.Entry<String, String> e : r.contents().entrySet()) {
                String content = e.getValue();
                if (content == null || content.isBlank()) continue;

                for (String rawLine : content.split("\n")) {
                    String w = normalizeWord(rawLine);
                    if (w != null) keywords.add(w);
                }
            }

            // 5) 构建 WordsSearchEx 并原子替换
            WordsSearchEx engine = new WordsSearchEx();
            engine.SetKeywords(List.copyOf(keywords));

            holder.swap(engine, manifest.getVersion(), keywords.size());

            log.info("Sensitive words loaded: version={}, files={}, keywords={}",
                    manifest.getVersion(), manifest.getFiles().size(), keywords.size());

        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Sensitive words reload error", e);
            throw new GloboxApplicationException(GovernanceCode.SENSITIVE_WORDS_RELOAD_ERROR);
        }
    }

    @Override
    public boolean containsSensitiveWords(String text)  {
        if (text == null || text.isBlank()) {
            return false;
        }
        WordsSearchEx engine = holder.engine();
        // toolgood-words 常用检测：ContainsAny
        return engine != null && engine.ContainsAny(text);
    }


    private static String normalizeWord(String line) {
        if (line == null) {
            return null;
        }
        String w = line.trim();
        if (w.isEmpty()) {
            return null;
        }

        // 去 BOM
        if (w.charAt(0) == '\uFEFF') {
            w = w.substring(1).trim();
        }

        // 忽略注释
        if (w.startsWith("#") || w.startsWith("//")) {
            return null;
        }

        return w.isEmpty() ? null : w;
    }
}
