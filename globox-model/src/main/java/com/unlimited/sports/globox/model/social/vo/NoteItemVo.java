package com.unlimited.sports.globox.model.social.vo;

import com.unlimited.sports.globox.common.vo.SearchDocumentDto;
import com.unlimited.sports.globox.common.vo.SearchResultItem;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记列表项视图
 */
@Slf4j
@Data
@Schema(description = "笔记列表项视图")
public class NoteItemVo {

    @Schema(description = "笔记ID", example = "1")
    private Long noteId;

    @Schema(description = "作者ID", example = "1")
    private Long userId;

    @Schema(description = "作者昵称", example = "这个是昵称")
    private String nickName;

    @Schema(description = "作者头像URL", example = "https://globox-dev-1386561970.cos.ap-chengdu.myqcloud.com/avatar/2026-01-03/c2cdd9219824420fa6c8956f760197de.jpg")
    private String avatarUrl;

    @Schema(description = "标题", example = "今天打球收获分享")
    private String title;

    @Schema(description = "正文（截断）", example = "今天练习了发球和截击...")
    private String content;

    @Schema(description = "封面图URL（首图或视频封面）", example = "https://cdn.example.com/note/1.jpg")
    private String coverUrl;

    @Schema(description = "媒体类型", example = "IMAGE")
    private String mediaType;

    @Schema(description = "点赞数", example = "10")
    private Integer likeCount;

    @Schema(description = "评论数", example = "5")
    private Integer commentCount;

    @Schema(description = "状态", example = "PUBLISHED")
    private String status;

    @Schema(description = "创建时间", example = "2025-12-26T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "标签代码列表", example = "[\"TENNIS_COMMUNITY\", \"EQUIPMENT_REVIEW\"]")
    private List<String> tags;

    @Schema(description = "标签描述列表", example = "[\"网球社区\", \"装备测评\"]")
    private List<String> tagsDesc;

    @Schema(description = "当前用户是否已点赞", example = "true")
    private Boolean liked;

    /**
     * 从搜索文档DTO转换为笔记列表项
     *
     * @param searchDocDto 搜索文档DTO
     * @param userInfo 用户信息（昵称、头像）
     * @return 笔记搜索结果项
     */
    public static SearchResultItem<NoteItemVo> fromSearchDocument(SearchDocumentDto searchDocDto, UserInfoVo userInfo) {
        log.info("dto: {}", searchDocDto);

        // 创建NoteItemVo
        NoteItemVo noteItem = new NoteItemVo();
        noteItem.setNoteId(searchDocDto.getBusinessId());
        noteItem.setUserId(Long.parseLong(searchDocDto.getCreatorId()));
        noteItem.setNickName(userInfo != null ? userInfo.getNickName() : null);
        noteItem.setAvatarUrl(userInfo != null ? userInfo.getAvatarUrl() : null);
        noteItem.setTitle(searchDocDto.getTitle());

        // 截断内容（最多150字符）
        String content = searchDocDto.getContent();
        if (content != null && content.length() > 150) {
            content = content.substring(0, 150) + "...";
        }
        noteItem.setContent(content);

        noteItem.setCoverUrl(searchDocDto.getCoverUrl());
        noteItem.setMediaType(searchDocDto.getNoteMediaType() != null ? searchDocDto.getNoteMediaType() : "IMAGE");
        noteItem.setLikeCount(searchDocDto.getLikes() != null ? searchDocDto.getLikes() : 0);
        noteItem.setCommentCount(searchDocDto.getComments() != null ? searchDocDto.getComments() : 0);
        noteItem.setStatus(searchDocDto.getStatus() != null ? searchDocDto.getStatus() : "PUBLISHED");
        noteItem.setCreatedAt(searchDocDto.getCreatedAt());
        noteItem.setLiked(false); // 默认未点赞，需要调用方根据当前用户设置

        // 返回搜索结果项
        return SearchResultItem.<NoteItemVo>builder()
                .dataType(searchDocDto.getDataType())
                .data(noteItem)
                .build();
    }
}

