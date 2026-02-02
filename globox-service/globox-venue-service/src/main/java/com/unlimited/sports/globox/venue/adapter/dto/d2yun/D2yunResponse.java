package com.unlimited.sports.globox.venue.adapter.dto.d2yun;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * D2yun平台统一响应格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class D2yunResponse<T> {

    /**
     * 状态码（0表示成功）
     */
    private Integer status;

    /**
     * 消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;
}
