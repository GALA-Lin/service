package com.unlimited.sports.globox.search.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.model.auth.vo.UserListItemVo;
import com.unlimited.sports.globox.search.document.UserSearchDocument;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户搜索服务接口
 * 用于用户搜索（通过昵称、球盒号搜索）
 */
public interface IUserSearchService {

    /**
     * 搜索用户
     * @param keyword 关键词（匹配昵称或球盒号）
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 用户搜索结果分页
     */
    PaginationResult<UserListItemVo> searchUsers(String keyword, Integer page, Integer pageSize);

    /**
     * 同步用户数据到ES
     * @param updatedTime 增量同步的时间点（为null则全量同步）
     * @return 同步的记录数
     */
    int syncUserData(LocalDateTime updatedTime);

    /**
     * 批量获取用户详情（从ES获取）
     * 用于帖子、约球等场景需要展示用户头像、昵称等信息
     * @param userIds 用户ID列表
     * @return Map<userId, UserSearchDocument>
     */
    Map<Long, UserSearchDocument> getUsersByIds(List<Long> userIds);

    /**
     * 获取单个用户详情（从ES获取）
     * @param userId 用户ID
     * @return UserSearchDocument 或 null
     */
    UserSearchDocument getUserById(Long userId);

    /**
     * 将UserSearchDocument转换为UserListItemVo
     * @param document 用户搜索文档
     * @return 用户列表项视图对象
     */
    UserListItemVo toListItemVo(UserSearchDocument document);
}
