package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 社交模块相关响应码枚举
 * 错误码区间：3000-3099（社交模块&&RallyResultEnum专用）  26-01-28 使用到3059
 */
@Getter
@AllArgsConstructor
public enum SocialCode implements ResultCode {

    NOTE_CONTENT_EMPTY(3001, "正文不能为空"),
    NOTE_MEDIA_EMPTY(3002, "媒体不能为空"),
    NOTE_MEDIA_TOO_MANY(3003, "图片最多9张，视频仅1条"),
    NOTE_MEDIA_TYPE_INVALID(3004, "媒体类型不合法，图片和视频不可混排"),
    NOTE_VIDEO_COVER_REQUIRED(3005, "视频必须提供封面图"),
    NOTE_NOT_FOUND(3006, "笔记不存在或已删除"),
    NOTE_PERMISSION_DENIED(3007, "无权限操作，仅作者可编辑/删除"),
    NOTE_STATUS_INVALID(3008, "笔记状态无效"),
    NOTE_CURSOR_INVALID(3009, "游标格式错误"),
    NOTE_PAGE_SIZE_EXCEEDED(3011, "每页数量不能超过50"),
    NOTE_SORT_INVALID(3012, "排序方式无效，仅支持pool/latest/hot"),
    NOTE_UPLOAD_FILE_FAILED(3013, "文件上传失败"),
    NOTE_UPLOAD_FILE_TOO_LARGE(3014, "文件大小超过限制"),
    NOTE_UPLOAD_FILE_TYPE_NOT_SUPPORTED(3015, "文件类型不支持"),
    NOTE_MEDIA_CLEAR_NOT_ALLOWED(3016, "不允许清空笔记的所有媒体"),
    NOTE_MEDIA_NOT_FOUND(3017, "媒体不存在"),
    NOTE_DRAFT_EMPTY(3018, "草稿不能完全为空，标题、正文、媒体至少填写一项"),
    NOTE_PUBLISH_VALIDATION_FAILED(3020, "发布失败：正文和媒体不能为空"),
    NOTE_SYNC_FAILED(3019, "笔记数据同步失败"),
    NOTE_DRAFT_NOT_FOUND(3020, "草稿不存在或已删除"),

    COMMENT_CONTENT_EMPTY(3021, "评论内容不能为空"),
    COMMENT_CLOSED(3022, "评论已关闭"),
    COMMENT_NOT_FOUND(3023, "评论不存在或已删除"),
    COMMENT_PERMISSION_DENIED(3024, "无权限删除评论"),
    COMMENT_PARENT_INVALID(3025, "父评论不存在或不属于该笔记"),
    COMMENT_CURSOR_INVALID(3026, "评论游标格式错误"),
    COMMENT_PAGE_SIZE_EXCEEDED(3027, "每页数量不能超过50"),

    NOTE_DRAFT_MEDIA_REQUIRED(3028, "草稿必须有媒体"),
    NOTE_ID_REQUIRED(3029, "笔记ID不能为空"),

    FOLLOW_SELF_NOT_ALLOWED(3030, "不允许关注自己"),
    USER_BLOCKED(3031, "你已被对方拉黑或已拉黑对方"),
    FOLLOW_DISABLED_BY_BLOCK(3032, "拉黑状态下无法关注"),

    IMPORT_USER_TO_IM_FAILED(3033, "用户注册即时通讯系统失败"),
    RELATION_RPC_FAILED(3034, "关注关系RPC调用失败"),
    USER_NOT_FOUND(3035, "用户不存在"),
    RALLY_CREATE_FAILED(3036, "创建社交活动失败，请稍后重试"),
    RALLY_EVENT_DATE_BEFORE_NOW(3037, "社交活动创建时间早于当前时间，请重新核对时间"),
    INVALID_TAG_CODE(3038, "无效的标签代码"),
    FOLLOWER_COUNT_FETCH_FAILED(3039,"粉丝数量获取失败"),
    USER_NOTE_STATISTIC_ERROR(3040,"获取用户帖子统计失败"),


    ;

    private final Integer code;
    private final String message;
}
