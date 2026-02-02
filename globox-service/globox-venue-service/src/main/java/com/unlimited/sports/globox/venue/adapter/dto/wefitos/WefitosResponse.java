package com.unlimited.sports.globox.venue.adapter.dto.wefitos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wefitos平台统一响应格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WefitosResponse<T> {

    /**
     * 操作代码
     */
    private String code;

    /**
     * 状态码
     */
    private Integer cn;

    /**
     * 消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;
}
