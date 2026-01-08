package com.unlimited.sports.globox.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.unlimited.sports.globox.model.notification.entity.PushRecords;

import java.util.List;

/**
 * 推送记录服务接口
 */
public interface IPushRecordsService extends IService<PushRecords> {

    /**
     * 批量插入推送记录
     *
     * @param records 推送记录列表
     * @return 是否插入成功
     */
    boolean saveBatchRecords(List<PushRecords> records);
}
