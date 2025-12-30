package com.unlimited.sports.globox.merchant.service.impl;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.*;
import com.unlimited.sports.globox.merchant.service.MerchantSlotLockService;
import com.unlimited.sports.globox.model.merchant.dto.SlotLockRequestDto;
import com.unlimited.sports.globox.model.merchant.entity.*;
import com.unlimited.sports.globox.model.merchant.enums.LockTypeEnum;
import com.unlimited.sports.globox.model.merchant.enums.SlotRecordStatusEnum;
import com.unlimited.sports.globox.model.merchant.vo.BatchLockResultVo;
import com.unlimited.sports.globox.model.merchant.vo.BatchUnlockResultVo;
import com.unlimited.sports.globox.model.merchant.vo.BookedSlotInfoVo;
import com.unlimited.sports.globox.model.merchant.vo.LockedSlotVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long lockSlotByMerchant(Long templateId, LocalDate bookingDate,
                                   String reason, Long merchantId) {
        // 1. 校验模板并获取权限
        validateTemplateAndPermission(templateId, merchantId);

        // 2. 查询是否已有记录
        VenueBookingSlotRecord existingRecord = recordMapper.selectByTemplateAndDate(
                templateId, bookingDate);

        if (existingRecord != null) {
            // 记录已存在，需要检查状态
            return handleExistingRecord(existingRecord, reason, merchantId);
        } else {
            // 记录不存在，创建新的锁场记录
            return createLockedRecord(templateId, bookingDate, reason, merchantId);
        }
    }

    /**
     * 处理已存在的记录
     */
    private Long handleExistingRecord(VenueBookingSlotRecord record, String reason, Long merchantId) {
        Integer status = record.getStatus();
        Integer lockedType = record.getLockedType();

        // 情况1: 记录状态为 AVAILABLE (可预订)
        if (status.equals(AVAILABLE.getCode())) {
            // 直接锁定
            record.setStatus(UNAVAILABLE.getCode());
            record.setLockedType(MERCHANT.getCode());
            record.setLockReason(reason);
            record.setUpdatedAt(LocalDateTime.now());
            recordMapper.updateById(record);

            log.info("商家ID: {} 锁定了可用时段，记录ID: {}", merchantId, record.getBookingSlotRecordId());
            return record.getBookingSlotRecordId();
        }

        // 情况2: 已被商家锁定
        if (status.equals(UNAVAILABLE.getCode()) &&
                lockedType != null && lockedType.equals(MERCHANT.getCode())) {
            throw new GloboxApplicationException("该时段已被商家锁定");
        }

        // 情况3: 用户已预订（lockedType = 1 或有订单信息）
        if (record.getOrderId() != null ||
                (lockedType != null && lockedType.equals(1))) {
            // 获取订单信息用于提示
            String orderInfo = buildOrderInfoMessage(record);
            throw new GloboxApplicationException(
                    String.format("该时段已被用户预订，%s。请先联系客户取消订单后再锁定场地", orderInfo)
            );
        }

        // 情况4: 其他不可用状态
        throw new GloboxApplicationException(
                String.format("该时段当前状态为【%s】，无法锁定", getStatusName(status))
        );
    }

    /**
     * 创建新的锁场记录
     */
    private Long createLockedRecord(Long templateId, LocalDate bookingDate,
                                    String reason, Long merchantId) {
        VenueBookingSlotRecord newRecord = VenueBookingSlotRecord.builder()
                .slotTemplateId(templateId)
                .bookingDate(bookingDate)
                .status(UNAVAILABLE.getCode())
                .lockedType(MERCHANT.getCode())
                .lockReason(reason)
                .operatorSource(MERCHANT.getCode())
                .operatorId(merchantId)
                .build();

        recordMapper.insert(newRecord);

        log.info("商家ID: {} 创建并锁定了新记录ID: {}, 模板ID: {}, 日期: {}, 原因: {}",
                merchantId, newRecord.getBookingSlotRecordId(), templateId, bookingDate, reason);

        return newRecord.getBookingSlotRecordId();
    }

    /**
     * 构建订单信息提示
     */
    private String buildOrderInfoMessage(VenueBookingSlotRecord record) {
        StringBuilder IdBuilder = new StringBuilder();
        if (record.getOrderId() != null) {
            IdBuilder.append("订单ID: ").append(record.getOrderId());
        }

        return !IdBuilder.isEmpty() ? IdBuilder.toString() : "订单信息未知";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchLockResultVo lockSlotsBatchByMerchant(List<SlotLockRequestDto> requests,
                                                      String reason, Long merchantId) {
        if (requests == null || requests.isEmpty()) {
            throw new GloboxApplicationException("请选择要锁定的时段");
        }

        int successCount = 0;
        int bookedCount = 0; // 已被预订的数量
        List<String> errorMessages = new ArrayList<>();
        List<BookedSlotInfoVo> bookedSlots = new ArrayList<>();

        for (SlotLockRequestDto request : requests) {
            try {
                lockSlotByMerchant(request.getTemplateId(), request.getBookingDate(),
                        reason, merchantId);
                successCount++;
            } catch (GloboxApplicationException e) {
                String errorMsg = e.getMessage();

                // 特殊处理：如果是已被预订的情况
                if (errorMsg.contains("已被用户预订")) {
                    bookedCount++;
                    // 尝试获取详细信息
                    VenueBookingSlotRecord record = recordMapper.selectByTemplateAndDate(
                            request.getTemplateId(), request.getBookingDate());
                    if (record != null && record.getOrderId() != null) {
                        bookedSlots.add(BookedSlotInfoVo.builder()
                                .templateId(request.getTemplateId())
                                .bookingDate(request.getBookingDate())
                                .orderId(record.getOrderId())
                                .build());
                    }
                }

                errorMessages.add(String.format("模板ID: %d, 日期: %s - %s",
                        request.getTemplateId(), request.getBookingDate(), errorMsg));
                log.warn("锁定失败: 模板ID: {}, 日期: {}, 原因: {}",
                        request.getTemplateId(), request.getBookingDate(), errorMsg);
            } catch (Exception e) {
                errorMessages.add(String.format("模板ID: %d, 日期: %s - 系统错误: %s",
                        request.getTemplateId(), request.getBookingDate(), e.getMessage()));
                log.error("锁定异常: 模板ID: {}, 日期: {}",
                        request.getTemplateId(), request.getBookingDate(), e);
            }
        }

        log.info("商家ID: {} 批量锁定完成，总数: {}, 成功: {}, 失败: {}, 已被预订: {}",
                merchantId, requests.size(), successCount,
                requests.size() - successCount, bookedCount);

        return BatchLockResultVo.builder()
                .totalCount(requests.size())
                .successCount(successCount)
                .failed(requests.size() - successCount)
                .bookedCount(bookedCount)
                .errorMessages(errorMessages)
                .bookedSlots(bookedSlots)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockSlotByMerchant(Long recordId, Long merchantId) {
        // 1. 查询记录
        VenueBookingSlotRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new GloboxApplicationException("时段记录不存在");
        }

        // 2. 权限校验
        validateMerchantPermission(record.getSlotTemplateId(), merchantId);

        // 3. 状态校验
        if (!record.getStatus().equals(UNAVAILABLE.getCode())) {
            throw new GloboxApplicationException("该时段未被锁定，无需解锁");
        }

        if (record.getLockedType() == null || !record.getLockedType().equals(MERCHANT.getCode())) {
            throw new GloboxApplicationException(
                    String.format("该时段不是商家锁定的（当前锁定类型: %s），无法解锁",
                            getLockTypeName(record.getLockedType()))
            );
        }

        // 4. 解锁：如果是纯锁场记录（无订单关联），直接删除；否则更新状态
        if (record.getOrderId() == null) {
            // 纯商家锁场，直接删除记录
            recordMapper.deleteById(recordId);
            log.info("商家ID: {} 删除了锁场记录ID: {}", merchantId, recordId);
        } else {
            // 有订单关联（理论上不应该出现，因为上面已经检查了lockedType）
            // 保守处理：更新为可用状态
            record.setStatus(AVAILABLE.getCode());
            record.setLockedType(null);
            record.setLockReason(null);
            record.setUpdatedAt(LocalDateTime.now());
            recordMapper.updateById(record);
            log.info("商家ID: {} 解锁了记录ID: {}（保留记录）", merchantId, recordId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchUnlockResultVo unlockSlotsBatchByMerchant(List<Long> recordIds, Long merchantId) {
        if (recordIds == null || recordIds.isEmpty()) {
            throw new GloboxApplicationException("请选择要解锁的时段");
        }

        int successCount = 0;
        List<String> errorMessages = new ArrayList<>();

        for (Long recordId : recordIds) {
            try {
                unlockSlotByMerchant(recordId, merchantId);
                successCount++;
            } catch (Exception e) {
                String errorMsg = String.format("记录ID: %d - %s", recordId, e.getMessage());
                errorMessages.add(errorMsg);
                log.warn("解锁失败: {}", errorMsg);
            }
        }

        log.info("商家ID: {} 批量解锁完成，成功: {}/{}",
                merchantId, successCount, recordIds.size());

        return BatchUnlockResultVo.builder()
                .total(recordIds.size())
                .success(successCount)
                .failed(recordIds.size() - successCount)
                .errorMessages(errorMessages)
                .build();
    }

    @Override
    public List<LockedSlotVo> queryLockedSlots(Long courtId, Long venueId,
                                               LocalDate startDate, LocalDate endDate,
                                               Integer lockedType) {
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
            result = recordMapper.selectLockedSlotsByCourtAndDateRange(
                    courtId, startDate, endDate);
        } else {
            result = recordMapper.selectLockedSlotsByVenueAndDateRange(
                    venueId, startDate, endDate);
        }

        // 过滤锁定类型
        if (lockedType != null) {
            result = result.stream()
                    .filter(vo -> vo.getLockedBy().equals(lockedType))
                    .collect(Collectors.toList());
        }

        log.info("查询锁定时段，场地ID: {}, 场馆ID: {}, 日期范围: {}~{}, 锁定类型: {}, 结果数: {}",
                courtId, venueId, startDate, endDate, lockedType, result.size());

        return result;
    }

    /**
     * 校验模板并验证商家权限
     */
    private VenueBookingSlotTemplate validateTemplateAndPermission(Long templateId, Long merchantId) {
        VenueBookingSlotTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new GloboxApplicationException("时段模板不存在");
        }

        Court court = courtMapper.selectById(template.getCourtId());
        if (court == null) {
            throw new GloboxApplicationException("场地不存在");
        }

        Venue venue = venueMapper.selectById(court.getVenueId());
        if (venue == null || !venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该时段");
        }

        return template;
    }

    /**
     * 校验商家权限
     */
    private void validateMerchantPermission(Long templateId, Long merchantId) {
        validateTemplateAndPermission(templateId, merchantId);
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(Integer statusCode) {
        SlotRecordStatusEnum status = SlotRecordStatusEnum.getByCode(statusCode);
        return status != null ? status.getName() : "未知";
    }

    /**
     * 获取锁定类型名称
     */
    private String getLockTypeName(Integer lockedType) {
        if (lockedType == null) {
            return "无";
        }
        LockTypeEnum typeEnum = LockTypeEnum.getByCode(lockedType);
        return typeEnum != null ? typeEnum.getDesc() : "未知类型";
    }
}