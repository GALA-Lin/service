package com.unlimited.sports.globox.dubbo.social;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.model.social.dto.NoteStatisticsDto;
import com.unlimited.sports.globox.model.social.vo.NoteSyncVo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 笔记搜索数据RPC服务接口
 * 供搜索服务调用，获取笔记数据用于增量同步到Elasticsearch
 */
public interface INoteSearchDataService {

    /**
     * 增量同步笔记数据
     * 业务逻辑：
     * @param updatedTime 上一次同步的时间戳，为null表示全量同步，不为null表示增量同步
     * @return 笔记同步数据列表（NoteSyncVO格式）
     */
    RpcResult<List<NoteSyncVo>> syncNoteData(LocalDateTime updatedTime);

    /**
     * 批量查询用户对笔记的点赞状态
     * 综合查询数据库和Redis中未同步的点赞事件
     * 只返回用户已点赞的笔记ID集合
     *
     * @param userId 用户ID
     * @param noteIds 笔记ID列表
     * @return 用户已点赞的笔记ID集合（包括数据库和Redis中未同步的）
     */
    RpcResult<Set<Long>> queryUserLikedNoteIds(Long userId, List<Long> noteIds);

    /**
     * 批量查询笔记统计信息（点赞数、评论数、用户是否点赞）
     * 一次性查询多个笔记的统计信息，减少数据库和Redis访问次数
     * 综合查询数据库和Redis中未同步的事件信息
     *
     * @param noteIds 笔记ID列表
     * @param userId 当前用户ID（可选，为null时不查询点赞状态）
     * @return 笔记统计信息Map，key为noteId，value为NoteStatisticsDto
     */
    RpcResult<Map<Long, NoteStatisticsDto>> queryNotesStatistics(List<Long> noteIds, Long userId);
}
