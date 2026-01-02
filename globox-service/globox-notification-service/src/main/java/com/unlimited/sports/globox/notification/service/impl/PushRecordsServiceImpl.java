package com.unlimited.sports.globox.notification.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unlimited.sports.globox.model.notification.entity.PushRecords;
import com.unlimited.sports.globox.notification.mapper.PushRecordsMapper;
import com.unlimited.sports.globox.notification.service.IPushRecordsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 推送记录服务实现
 */
@Slf4j
@Service
public class PushRecordsServiceImpl extends ServiceImpl<PushRecordsMapper, PushRecords> implements IPushRecordsService {

    @Override
    @Transactional
    public boolean saveBatchRecords(List<PushRecords> records) {
        return saveBatch(records);
    }
}
