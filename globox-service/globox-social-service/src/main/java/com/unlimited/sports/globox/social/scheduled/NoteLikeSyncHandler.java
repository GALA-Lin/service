package com.unlimited.sports.globox.social.scheduled;

import com.unlimited.sports.globox.social.config.XxlJobProperties;
import com.unlimited.sports.globox.social.service.NoteLikeSyncService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 笔记点赞同步定时任务
 * 
 * 负责：
 * 1. 将 Redis Hash 中的点赞事件刷到 DB
 * 2. 将脏 noteId 的互动数据（likeCount、commentCount、collectCount）同步到搜索服务
 */
@Slf4j
@Component
public class NoteLikeSyncHandler {

    @Autowired
    private NoteLikeSyncService noteLikeSyncService;

    @Autowired
    private XxlJobProperties xxlJobProperties;

    /**
     * XXL-JOB 定时任务（5分钟）：
     * 1. 将 Redis Hash 中的点赞事件刷到 DB
     * 2. 将脏 noteId 的互动数据（likeCount、commentCount、collectCount）同步到搜索服务
     */
    @XxlJob("noteLikeSyncJobHandler")
    public void syncJob() {
        log.debug("[定时任务] 开始执行笔记点赞同步任务");
        try {
            noteLikeSyncService.executeSync();
            log.debug("[定时任务] 笔记点赞同步任务执行完成");
        } catch (Exception e) {
            log.error("[定时任务] 笔记点赞同步任务执行失败", e);
            throw e;
        }
    }
}
