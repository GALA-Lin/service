package com.unlimited.sports.globox.payment;

import java.util.UUID;

/**
 * 生成 UUID
 * TODO ETA 2026/01/04 删除测试工具
 */
public class GetUUID {
    public static void main(String[] args) {
        System.out.println(UUID.randomUUID().toString().replace("-", ""));
    }
}
