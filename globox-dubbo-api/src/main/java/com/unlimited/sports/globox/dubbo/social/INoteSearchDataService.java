package com.unlimited.sports.globox.dubbo.social;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.model.social.vo.NoteSyncVo;

import java.time.LocalDateTime;
import java.util.List;

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
}
