package com.unlimited.sports.globox.venue.adapter.dto.aitennis;

import lombok.Data;

/**
 * 爱网球统一响应
 */
@Data
public class AitennisResponse<T> {

    /**
     * 响应码：0表示成功
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;
}
