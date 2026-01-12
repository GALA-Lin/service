package com.unlimited.sports.globox.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.lock.RedisDistributedLock;
import com.unlimited.sports.globox.merchant.mapper.*;
import com.unlimited.sports.globox.merchant.service.MerchantSlotLockService;
import com.unlimited.sports.globox.model.merchant.entity.*;
import com.unlimited.sports.globox.model.merchant.enums.SlotRecordStatusEnum;
import com.unlimited.sports.globox.model.merchant.vo.LockedSlotVo;
import com.unlimited.sports.globox.venue.constants.BookingCacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.unlimited.sports.globox.model.merchant.enums.OperatorSourceEnum.*;
import static com.unlimited.sports.globox.model.merchant.enums.SlotRecordStatusEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantSlotLockServiceImpl implements MerchantSlotLockService {

    private final MerchantVenueBookingSlotRecordMapper recordMapper;
    private final MerchantVenueBookingSlotTemplateMapper templateMapper;
    private final CourtMapper courtMapper;
    private final VenueMapper venueMapper;
    private final RedisDistributedLock redisDistributedLock;

    @Override
    public void lockSlotByMerchant(Long templateId, LocalDate bookingDate, String reason, Long merchantId) {
        // 1. 验证权限（在获取锁之前，快速失败）
        validateMerchantPermission(templateId, merchantId);

        // 2. 构建分布式锁key
        String lockKey = buildLockKey(templateId, bookingDate);
        List<String> lockKeys = List.of(lockKey);

        List<RLock> locks = null;

        try {
            // 3. 获取分布式锁
            locks = redisDistributedLock.tryLockMultiple(lockKeys, 1, -1L, TimeUnit.SECONDS);
            if (locks == null) {
                log.warn("获取分布式锁失败 - merchantId: {}, templateId: {}, date: {}",
                        merchantId, templateId, bookingDate);
                throw new GloboxApplicationException("该时段正在被操作，请稍后重试");
            }
            log.info("【锁包事务】成功获取分布式锁 - merchantId: {}, templateId: {}, date: {}",
                    merchantId, templateId, bookingDate);

            // 4. 在事务中执行锁定操作
            executeSlotLockInTransaction(templateId, bookingDate, reason, merchantId);

            log.info("事务执行成功 - merchantId: {}, templateId: {}", merchantId, templateId);

        } finally {
            // 5. 释放锁（无论事务成功还是失败）
            if (locks != null) {
                redisDistributedLock.unlockMultiple(locks);
                log.info("【锁包事务】释放分布式锁 - merchantId: {}", merchantId);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void executeSlotLockInTransaction(Long templateId, LocalDate bookingDate,
                                             String reason, Long merchantId) {
        // 1. 查询模板（事务内再次验证）
        VenueBookingSlotTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new GloboxApplicationException("槽位模板不存在");
        }

        // 2. 查询或创建记录
        VenueBookingSlotRecord record = recordMapper.selectOne(
                new LambdaQueryWrapper<VenueBookingSlotRecord>()
                        .eq(VenueBookingSlotRecord::getSlotTemplateId, templateId)
                        .eq(VenueBookingSlotRecord::getBookingDate, bookingDate)
        );

        if (record != null) {
            // 记录已存在，检查状态
            if (!record.getStatus().equals(AVAILABLE.getCode())) {
                throw new GloboxApplicationException(
                        String.format("该时段当前状态为【%s】，无法锁定",
                                getStatusName(record.getStatus()))
                );
            }

            // 更新状态为锁定
            record.setStatus(UNAVAILABLE.getCode());
            record.setLockedType(MERCHANT.getCode());
            record.setLockReason(reason);
            recordMapper.updateById(record);

            log.info("更新时段记录为锁定状态 - recordId: {}, templateId: {}, date: {}",
                    record.getBookingSlotRecordId(), templateId, bookingDate);
        } else {
            // 记录不存在，创建新记录并直接设为锁定状态
            record = new VenueBookingSlotRecord();
            record.setSlotTemplateId(templateId);
            record.setBookingDate(bookingDate);
            record.setStatus(UNAVAILABLE.getCode());
            record.setLockedType(MERCHANT.getCode());
            record.setLockReason(reason);
            record.setOperatorSource(MERCHANT.getCode());
            recordMapper.insert(record);

            log.info("创建新记录并锁定 - templateId: {}, date: {}, recordId: {}",
                    templateId, bookingDate, record.getBookingSlotRecordId());
        }

        log.info("商家ID: {} 锁定了模板ID: {}, 日期: {}, 原因: {}",
                merchantId, templateId, bookingDate, reason);
    }

    @Override
    public void lockSlotsBatchByMerchant(List<Long> templateIds, LocalDate bookingDate,
                                         String reason, Long merchantId) {
        if (templateIds == null || templateIds.isEmpty()) {
            throw new GloboxApplicationException("请选择要锁定的时段");
        }

        if (bookingDate == null) {
            throw new GloboxApplicationException("预约日期不能为空");
        }

        // 1. 批量验证权限（在获取锁之前）
        for (Long templateId : templateIds) {
            validateMerchantPermission(templateId, merchantId);
        }

        // 2. 构建所有锁的key
        List<String> lockKeys = templateIds.stream()
                .map(templateId -> buildLockKey(templateId, bookingDate))
                .collect(Collectors.toList());

        List<RLock> locks = null;

        try {
            // 3. 批量获取所有时段的锁
            locks = redisDistributedLock.tryLockMultiple(lockKeys, 1, -1L, TimeUnit.SECONDS);
            if (locks == null) {
                log.warn("批量获取分布式锁失败 - merchantId: {}, 时段数: {}",
                        merchantId, templateIds.size());
                throw new GloboxApplicationException("部分时段正在被操作，请稍后重试");
            }
            log.info("【锁包事务】成功批量获取分布式锁 - merchantId: {}, 时段数: {}",
                    merchantId, templateIds.size());

            // 4. 在事务中批量执行锁定操作
            executeBatchSlotLockInTransaction(templateIds, bookingDate, reason, merchantId);

            log.info("批量锁定事务执行成功 - merchantId: {}, 时段数: {}",
                    merchantId, templateIds.size());

        } finally {
            // 5. 释放所有锁
            if (locks != null) {
                redisDistributedLock.unlockMultiple(locks);
                log.info("【锁包事务】释放批量分布式锁 - merchantId: {}", merchantId);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void executeBatchSlotLockInTransaction(List<Long> templateIds, LocalDate bookingDate,
                                                  String reason, Long merchantId) {
        int successCount = 0;
        List<String> failedTemplates = new ArrayList<>();

        for (Long templateId : templateIds) {
            try {
                // 使用单个锁定的事务逻辑（但不再获取锁，因为外层已经获取）
                executeSlotLockInTransaction(templateId, bookingDate, reason, merchantId);
                successCount++;
            } catch (Exception e) {
                log.warn("锁定模板ID: {}, 日期: {} 失败: {}",
                        templateId, bookingDate, e.getMessage());
                failedTemplates.add(templateId.toString());
            }
        }

        log.info("商家ID: {} 批量锁定完成，成功: {}/{}, 失败模板: {}",
                merchantId, successCount, templateIds.size(),
                failedTemplates.isEmpty() ? "无" : String.join(",", failedTemplates));

        // 如果全部失败，抛出异常回滚事务
        if (successCount == 0) {
            throw new GloboxApplicationException("批量锁定全部失败");
        }
    }

    @Override
    public void unlockSlotByMerchant(Long templateId, LocalDate bookingDate, Long merchantId) {
        // 1. 验证权限（在获取锁之前）
        validateMerchantPermission(templateId, merchantId);

        // 2. 构建分布式锁key
        String lockKey = buildLockKey(templateId, bookingDate);
        List<String> lockKeys = List.of(lockKey);

        List<RLock> locks = null;

        try {
            // 3. 获取分布式锁
            locks = redisDistributedLock.tryLockMultiple(lockKeys, 1, -1L, TimeUnit.SECONDS);
            if (locks == null) {
                log.warn("获取分布式锁失败 - merchantId: {}, templateId: {}, date: {}",
                        merchantId, templateId, bookingDate);
                throw new GloboxApplicationException("该时段正在被操作，请稍后重试");
            }
            log.info("【锁包事务】成功获取分布式锁 - merchantId: {}, templateId: {}, date: {}",
                    merchantId, templateId, bookingDate);

            // 4. 在事务中执行解锁操作
            executeSlotUnlockInTransaction(templateId, bookingDate, merchantId);

            log.info("解锁事务执行成功 - merchantId: {}, templateId: {}", merchantId, templateId);

        } finally {
            // 5. 释放锁
            if (locks != null) {
                redisDistributedLock.unlockMultiple(locks);
                log.info("【锁包事务】释放分布式锁 - merchantId: {}", merchantId);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void executeSlotUnlockInTransaction(Long templateId, LocalDate bookingDate,
                                               Long merchantId) {
        // 1. 查询记录
        VenueBookingSlotRecord record = recordMapper.selectOne(
                new LambdaQueryWrapper<VenueBookingSlotRecord>()
                        .eq(VenueBookingSlotRecord::getSlotTemplateId, templateId)
                        .eq(VenueBookingSlotRecord::getBookingDate, bookingDate)
        );

        if (record == null) {
            throw new GloboxApplicationException("时段记录不存在，无需解锁");
        }

        // 2. 状态校验
        if (!record.getStatus().equals(UNAVAILABLE.getCode())) {
            throw new GloboxApplicationException("该时段未被商家锁定，无需解锁");
        }

        if (record.getLockedType() == null || !record.getLockedType().equals(MERCHANT.getCode())) {
            throw new GloboxApplicationException("该时段不是商家锁定的，无法解锁");
        }

        // 3. 更新状态为可用
        record.setStatus(AVAILABLE.getCode());
        record.setLockedType(null);
        record.setLockReason(null);
        recordMapper.updateById(record);

        log.info("商家ID: {} 解锁了模板ID: {}, 日期: {}", merchantId, templateId, bookingDate);
    }

    @Override
    public void unlockSlotsBatchByMerchant(List<Long> templateIds, LocalDate bookingDate,
                                           Long merchantId) {
        if (templateIds == null || templateIds.isEmpty()) {
            throw new GloboxApplicationException("请选择要解锁的时段");
        }

        if (bookingDate == null) {
            throw new GloboxApplicationException("预约日期不能为空");
        }

        // 1. 批量验证权限（在获取锁之前）
        for (Long templateId : templateIds) {
            validateMerchantPermission(templateId, merchantId);
        }

        // 2. 构建所有锁的key
        List<String> lockKeys = templateIds.stream()
                .map(templateId -> buildLockKey(templateId, bookingDate))
                .collect(Collectors.toList());

        List<RLock> locks = null;

        try {
            // 3. 批量获取所有时段的锁
            locks = redisDistributedLock.tryLockMultiple(lockKeys, 1, -1L, TimeUnit.SECONDS);
            if (locks == null) {
                log.warn("批量获取分布式锁失败 - merchantId: {}, 时段数: {}",
                        merchantId, templateIds.size());
                throw new GloboxApplicationException("部分时段正在被操作，请稍后重试");
            }
            log.info("【锁包事务】成功批量获取分布式锁 - merchantId: {}, 时段数: {}",
                    merchantId, templateIds.size());

            // 4. 在事务中批量执行解锁操作
            executeBatchSlotUnlockInTransaction(templateIds, bookingDate, merchantId);

            log.info("批量解锁事务执行成功 - merchantId: {}, 时段数: {}",
                    merchantId, templateIds.size());

        } finally {
            // 5. 释放所有锁
            if (locks != null) {
                redisDistributedLock.unlockMultiple(locks);
                log.info("【锁包事务】释放批量分布式锁 - merchantId: {}", merchantId);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void executeBatchSlotUnlockInTransaction(List<Long> templateIds, LocalDate bookingDate,
                                                    Long merchantId) {
        int successCount = 0;
        List<String> failedTemplates = new ArrayList<>();

        for (Long templateId : templateIds) {
            try {
                executeSlotUnlockInTransaction(templateId, bookingDate, merchantId);
                successCount++;
            } catch (Exception e) {
                log.warn("解锁模板ID: {}, 日期: {} 失败: {}",
                        templateId, bookingDate, e.getMessage());
                failedTemplates.add(templateId.toString());
            }
        }

        log.info("商家ID: {} 批量解锁完成，成功: {}/{}, 失败模板: {}",
                merchantId, successCount, templateIds.size(),
                failedTemplates.isEmpty() ? "无" : String.join(",", failedTemplates));

        // 如果全部失败，抛出异常回滚事务
        if (successCount == 0) {
            throw new GloboxApplicationException("批量解锁全部失败");
        }
    }

    @Override
    public List<LockedSlotVo> queryLockedSlots(Long courtId, Long venueId,
                                               LocalDate startDate, LocalDate endDate,
                                               Integer lockedByType) {
        // 参数验证
        if (courtId == null && venueId == null) {
            throw new GloboxApplicationException("场地ID和场馆ID至少提供一个");
        }

        if (startDate == null || endDate == null) {
            throw new GloboxApplicationException("开始日期和结束日期不能为空");
        }

        if (startDate.isAfter(endDate)) {
            throw new GloboxApplicationException("开始日期不能晚于结束日期");
        }

        // 查询数据
        List<LockedSlotVo> result;
        if (courtId != null) {
            // 按场地查询
            result = recordMapper.selectLockedSlotsByCourtAndDateRange(courtId, startDate, endDate);
        } else {
            // 按场馆查询
            result = recordMapper.selectLockedSlotsByVenueAndDateRange(venueId, startDate, endDate);
        }

        // 过滤锁定类型
        if (lockedByType != null) {
            result = result.stream()
                    .filter(vo -> vo.getLockedBy().equals(lockedByType))
                    .collect(Collectors.toList());
        }

        log.info("查询锁定时段，场地ID: {}, 场馆ID: {}, 日期范围: {}~{}, 锁定类型: {}, 结果数: {}",
                courtId, venueId, startDate, endDate, lockedByType, result.size());

        return result;
    }

    /**
     * 校验商家权限
     */
    private void validateMerchantPermission(Long templateId, Long merchantId) {
        VenueBookingSlotTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new GloboxApplicationException("槽位模板不存在");
        }

        Court court = courtMapper.selectById(template.getCourtId());
        if (court == null) {
            throw new GloboxApplicationException("场地不存在");
        }

        Venue venue = venueMapper.selectById(court.getVenueId());
        if (venue == null || !venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该时段");
        }
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer statusCode) {
        SlotRecordStatusEnum status = SlotRecordStatusEnum.getByCode(statusCode);
        return status != null ? status.getName() : "未知";
    }

    /**
     * 构建锁键（与用户下单使用相同的key格式）
     */
    private String buildLockKey(Long templateId, LocalDate bookingDate) {
        return BookingCacheConstants.BOOKING_LOCK_KEY_PREFIX +
                templateId +
                BookingCacheConstants.BOOKING_LOCK_KEY_SEPARATOR +
                bookingDate;
    }
}