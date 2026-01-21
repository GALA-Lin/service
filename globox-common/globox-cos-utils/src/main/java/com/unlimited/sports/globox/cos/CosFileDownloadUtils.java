package com.unlimited.sports.globox.cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.unlimited.sports.globox.common.prop.CosProperties;
import com.unlimited.sports.globox.cos.dto.BatchDownloadResultDto;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * cos 文件下载 utils
 */
@Slf4j
public class CosFileDownloadUtils {

    public static String downloadFromCosToString(COSClient cosClient, CosProperties cosProperties, String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosProperties.getBucketName(), key);

        COSObject cosObject = null;
        try {
            cosObject = cosClient.getObject(getObjectRequest);

            try (InputStream in = cosObject.getObjectContent();
                 Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                 StringWriter writer = new StringWriter()) {

                char[] buf = new char[8192];
                int n;
                while ((n = reader.read(buf)) != -1) {
                    writer.write(buf, 0, n);
                }
                return writer.toString();
            }
        } catch (CosServiceException e) {
            // COS 服务端返回错误（权限/不存在/签名过期等）
            log.error("COS 处理失败：bucket={}, key={}, code={}, msg={}",
                    cosProperties.getBucketName(), key, e.getErrorCode(), e.getMessage(), e);
        } catch (CosClientException e) {
            // 客户端网络/超时/连接等问题
            log.error("COS 连接失败：bucket={}, key={}, msg={}",
                    cosProperties.getBucketName(), key, e.getMessage(), e);
        } catch (IOException e) {
            // 读取流失败
            log.error("COS 读取失败：bucket={}, key={}, msg={}",
                    cosProperties.getBucketName(), key, e.getMessage(), e);
        }

        return null;
    }



    /**
     * 并发批量下载
     *
     * @param parallelism 建议 4~16（根据服务带宽/CPU），太大反而可能导致超时/抖动
     * @param failFast    true：任意一个失败就尽早取消其它任务；false：尽量下载完所有
     */
    public static BatchDownloadResultDto batchDownloadFromCosToString(
            COSClient cosClient,
            CosProperties cosProperties,
            Collection<String> keys,
            int parallelism,
            boolean failFast
    ) {
        if (keys == null || keys.isEmpty()) {
            return new BatchDownloadResultDto(new HashMap<>(), new HashMap<>());
        }

        int p = Math.max(1, parallelism);
        ExecutorService pool = Executors.newFixedThreadPool(p);

        Map<String, String> contents = new ConcurrentHashMap<>();
        Map<String, Throwable> errors = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = keys.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(key -> CompletableFuture.runAsync(() -> {
                    try {
                        String text = downloadFromCosToString(cosClient, cosProperties, key);
                        if (text == null) {
                            throw new IllegalStateException("download result is null");
                        }
                        contents.put(key, text);
                    } catch (Throwable t) {
                        errors.put(key, t);
                        if (failFast) {
                            throw new CompletionException(t);
                        }
                    }
                }, pool))
                .toList();

        try {
            // 等待全部完成（failFast=true 时，任意任务抛错会在这里尽快体现）
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException ignore) {
            // failFast 场景下会走到这里：尽早取消其他任务
            futures.forEach(f -> f.cancel(true));
        } finally {
            pool.shutdownNow();
        }

        return new BatchDownloadResultDto(new HashMap<>(contents), new HashMap<>(errors));
    }

}
