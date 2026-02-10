package com.unlimited.sports.globox.search.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.model.search.entity.SearchSyncRecord;
import com.unlimited.sports.globox.search.config.XxlJobProperties;
import com.unlimited.sports.globox.search.mapper.SearchSyncRecordMapper;
import com.unlimited.sports.globox.search.service.INoteSearchService;
import com.unlimited.sports.globox.search.service.IUserSearchService;
import com.unlimited.sports.globox.search.service.IVenueSearchService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 搜索数据同步定时任务
 *
 * 每次执行时从 search_sync_record 表查询上次成功同步的时间作为增量起点。
 * 同步完成后写入新的记录，供下次任务使用。
 * 支持通过任务参数传入 "full" 触发全量同步。
 */
@Slf4j
@Component
public class SearchDataSyncHandler {

    private static final String DATA_TYPE_NOTE = "NOTE";
    private static final String DATA_TYPE_USER = "USER";
    private static final String DATA_TYPE_VENUE = "VENUE";

    @Autowired
    private INoteSearchService noteSearchService;

    @Autowired
    private IUserSearchService userSearchService;

    @Autowired
    private IVenueSearchService venueSearchService;

    @Autowired
    private SearchSyncRecordMapper searchSyncRecordMapper;

    @Autowired
    private XxlJobProperties xxlJobProperties;

    /**
     * 笔记数据同步任务
     */
    @XxlJob("noteDataSyncJobHandler")
    public void syncNoteData() {
        doSync(DATA_TYPE_NOTE, (updatedTime) -> noteSearchService.syncNoteData(updatedTime));
    }

    /**
     * 用户数据同步任务
     */
    @XxlJob("userDataSyncJobHandler")
    public void syncUserData() {
        doSync(DATA_TYPE_USER, (updatedTime) -> userSearchService.syncUserData(updatedTime));
    }

    /**
     * 场馆数据同步任务
     */
    @XxlJob("venueDataSyncJobHandler")
    public void syncVenueData() {
        doSync(DATA_TYPE_VENUE, (updatedTime) -> venueSearchService.syncVenueData(updatedTime));
    }

    /**
     * 统一同步流程：查上次时间 → 执行同步 → 写入记录
     */
    private void doSync(String dataType, SyncAction action) {
        LocalDateTime syncStartTime = LocalDateTime.now();
        LocalDateTime updatedTime = resolveUpdatedTime(dataType);
        log.info("[XXL-JOB] 开始同步{}: updatedTime={}", dataType, updatedTime);

        try {
            int count = action.execute(updatedTime);

            // 写入成功记录
            saveSyncRecord(dataType, syncStartTime, count,
                    SearchSyncRecord.STATUS_SUCCESS,
                    String.format("同步完成: %d 条", count));

            String msg = String.format("%s 同步完成: 同步数=%d", dataType, count);
            log.info("[XXL-JOB] {}", msg);
            XxlJobHelper.handleSuccess(msg);
        } catch (Exception e) {
            // 写入失败记录
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 450) {
                errorMsg = errorMsg.substring(0, 450);
            }
            saveSyncRecord(dataType, syncStartTime, 0,
                    SearchSyncRecord.STATUS_FAILED, errorMsg);

            log.error("[XXL-JOB] {} 同步失败", dataType, e);
            XxlJobHelper.handleFail(dataType + " 同步失败: " + e.getMessage());
        }
    }

    /**
     * 解析增量起点时间
     * 优先使用任务参数 "full" 触发全量，否则从 DB 查询上次成功同步时间
     */
    private LocalDateTime resolveUpdatedTime(String dataType) {
        String jobParam = XxlJobHelper.getJobParam();
        if (jobParam != null && !jobParam.isBlank()) {
            if ("full".equalsIgnoreCase(jobParam.trim())) {
                log.info("[XXL-JOB] 参数指定全量同步");
                return null;
            }
        }
        return getLastSyncTime(dataType);
    }

    /**
     * 查询指定类型最近一次成功同步的时间
     */
    private LocalDateTime getLastSyncTime(String dataType) {
        SearchSyncRecord record = searchSyncRecordMapper.selectOne(
                new LambdaQueryWrapper<SearchSyncRecord>()
                        .eq(SearchSyncRecord::getDataType, dataType)
                        .eq(SearchSyncRecord::getSyncStatus, SearchSyncRecord.STATUS_SUCCESS)
                        .orderByDesc(SearchSyncRecord::getSyncId)
                        .last("LIMIT 1")
        );
        return record != null ? record.getSyncTime() : null;
    }

    /**
     * 写入同步记录
     */
    private void saveSyncRecord(String dataType, LocalDateTime syncTime,
                                int syncCount, int syncStatus, String syncMessage) {
        try {
            SearchSyncRecord record = SearchSyncRecord.builder()
                    .dataType(dataType)
                    .syncTime(syncTime)
                    .syncCount(syncCount)
                    .syncStatus(syncStatus)
                    .syncMessage(syncMessage)
                    .createdAt(LocalDateTime.now())
                    .build();
            searchSyncRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("[XXL-JOB] 写入同步记录失败: dataType={}", dataType, e);
        }
    }

    @FunctionalInterface
    private interface SyncAction {
        int execute(LocalDateTime updatedTime);
    }
}
