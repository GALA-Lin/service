package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 社交模块相关响应码枚举
 * 错误码区间：3000-3099（社交模块专用）
 */
@Getter
@AllArgsConstructor
public enum SocialCode implements ResultCode {

    // 笔记相关 3001-3012
    NOTE_CONTENT_EMPTY(3001, "正文不能为空"),
    NOTE_MEDIA_EMPTY(3002, "媒体不能为空"),
    NOTE_MEDIA_TOO_MANY(3003, "图片最多9张，视频仅1条"),
    NOTE_MEDIA_TYPE_INVALID(3004, "媒体类型不合法，图片和视频不可混排"),
    NOTE_VIDEO_COVER_REQUIRED(3005, "视频必须提供封面图"),
    NOTE_NOT_FOUND(3006, "笔记不存在或已删除"),
    NOTE_PERMISSION_DENIED(3007, "无权限操作，仅作者可编辑/删除"),
    NOTE_STATUS_INVALID(3008, "笔记状态无效"),
    NOTE_CURSOR_INVALID(3009, "游标格式错误"),
    NOTE_POOL_SEED_REQUIRED(3010, "池子流必须提供seed参数"),
    NOTE_PAGE_SIZE_EXCEEDED(3011, "每页数量不能超过50"),
    NOTE_SORT_INVALID(3012, "排序方式无效，仅支持latest"),

    ;

    private final Integer code;
    private final String message;
}
