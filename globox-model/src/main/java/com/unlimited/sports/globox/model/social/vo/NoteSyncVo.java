package com.unlimited.sports.globox.model.social.vo;

import com.unlimited.sports.globox.model.social.entity.SocialNote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记同步VO - 用于从笔记服务同步数据到搜索服务
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteSyncVo implements Serializable {

    /**
     * 笔记ID
     */
    private Long noteId;

    /**
     * 作者ID
     */
    private Long userId;

    /**
     * 笔记标题
     */
    private String title;

    /**
     * 笔记正文内容
     */
    private String content;

    /**
     * 笔记标签列表 (如: ["TENNIS_COMMUNITY", "EQUIPMENT_REVIEW"])
     */
    private List<String> tags;

    /**
     * 封面图URL（首图或视频封面）
     */
    private String coverUrl;

    /**
     * 媒体类型: IMAGE / VIDEO
     */
    private String mediaType;

    /**
     * 点赞数 - 由笔记服务维护
     * 搜索服务同步时用此字段计算 hotScore
     */
    private Integer likeCount;

    /**
     * 评论数 - 由笔记服务维护
     * 搜索服务同步时用此字段计算 hotScore
     */
    private Integer commentCount;

    /**
     * 收藏数 - 由笔记服务维护
     * 搜索服务同步时用此字段计算 hotScore
     */
    private Integer collectCount;

    /**
     * 是否精选
     */
    private Boolean featured;

    /**
     * 笔记状态
     */
    private SocialNote.Status status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
