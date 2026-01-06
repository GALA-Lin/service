package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.dubbo.merchant.dto.*;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.enums.BookingSlotStatus;
import com.unlimited.sports.globox.model.venue.enums.OperatorSourceEnum;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import com.unlimited.sports.globox.venue.service.IVenueBookingService;
import com.unlimited.sports.globox.venue.service.IVenueBookingSlotRecordService;
import com.unlimited.sports.globox.venue.service.VenuePriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 场馆预订事务处理Service
 * 专门处理需要在分布式锁保护下执行的事务逻辑
 */
@Slf4j
@Service
public class VenueBookingService implements IVenueBookingService {

    @Autowired
    private VenueBookingSlotRecordMapper slotRecordMapper;

    @Autowired
    private IVenueBookingSlotRecordService slotRecordService;

    @Autowired
    private VenuePriceService venuePriceService;

    /**
     * 在分布式锁保护下执行事务性的预订和计价逻辑
     * 此方法会开启事务，事务快照在获取锁之后建立，确保能读取到其他已提交事务的最新数据
     *
     * @param dto 价格请求DTO
     * @param templates 槽位模板列表
     * @param venue 场馆对象（外部已查询）
     * @param courtNameMap 场地名称映射
     * @param validateSlotsCallback 二次验证回调（复用外部的validateSlots方法）
     * @return 价格结果DTO
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public PricingResultDto executeBookingInTransaction(
            PricingRequestDto dto,
            List<VenueBookingSlotTemplate> templates,
            Venue venue,
            Map<Long, String> courtNameMap,
            Runnable validateSlotsCallback) {


        // 二次验证（获取锁后，事务内首次查询）- 确保槽位仍然可用
        // 因为事务在获取锁后才开启，所以这里能读取到其他已提交事务的最新数据
        // 使用回调复用外部的validateSlots方法
        validateSlotsCallback.run();

        // 原子性地更新槽位状态，防止超卖
        List<VenueBookingSlotRecord> records = lockBookingSlots(
                templates, dto.getUserId(), dto.getBookingDate());

        if (records == null) {
            log.warn("占用槽位失败 - 槽位已被其他用户占用 userId: {}", dto.getUserId());
            throw new GloboxApplicationException("槽位已被其他用户占用，请重新选择");
        }

        log.debug("槽位占用成功，recordIds: {}",
                records.stream()
                        .map(VenueBookingSlotRecord::getBookingSlotRecordId)
                        .toList());

        // 计算槽位价格
        List<RecordQuote> recordQuotes = venuePriceService.calculateSlotQuotes(
                records,
                venue.getVenueId(),
                venue.getName(),
                dto.getBookingDate(),
                courtNameMap
        );

        if (recordQuotes.isEmpty()) {
            log.warn("无法计算任何槽位的价格");
            throw new GloboxApplicationException("无法计算槽位价格");
        }

        // 计算额外费用
        BigDecimal totalBasePrice = recordQuotes.stream()
                .map(RecordQuote::getUnitPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OrderLevelExtraQuote> extraQuotes = venuePriceService.calculateExtraCharges(
                venue.getVenueId(),
                totalBasePrice,
                templates.size()
        );

        log.info("预订逻辑执行成功 - userId: {}, 槽位数: {}", dto.getUserId(), records.size());

        // 构建返回结果
        return PricingResultDto.builder()
                .recordQuote(recordQuotes)
                .orderLevelExtras(extraQuotes)
                .sourcePlatform(venue.getVenueType())
                .sellerName(venue.getName())
                .sellerId(venue.getVenueId())
                .bookingDate(dto.getBookingDate())
                .build();
    }

    /**
     * 占用预订槽位
     * 将槽位状态改为LOCKED_IN，并记录操作人信息
     *
     * @return 返回占用的槽位记录列表（按templates顺序）
     */
    private List<VenueBookingSlotRecord> lockBookingSlots(List<VenueBookingSlotTemplate> templates, Long userId, LocalDate bookingDate) {
        log.info("开始占用槽位 - userId: {}, 槽位数: {}", userId, templates.size());

        if (templates.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取所有模板ID
        List<Long> templateIds = templates.stream()
                .map(VenueBookingSlotTemplate::getBookingSlotTemplateId)
                .collect(Collectors.toList());

        // 批量查询已存在的记录
        List<VenueBookingSlotRecord> existingRecords = slotRecordMapper.selectByTemplateIdsAndDate(
                templateIds,
                bookingDate
        );

        // 构建已有记录的ID集合
        Set<Long> existingTemplateIds = existingRecords.stream()
                .map(VenueBookingSlotRecord::getSlotTemplateId)
                .collect(Collectors.toSet());

        // 保持 template 和 record 的对应关系
        Map<Long, VenueBookingSlotRecord> templateToRecordMap = new LinkedHashMap<>();
        List<VenueBookingSlotRecord> toInsert = new ArrayList<>();
        List<VenueBookingSlotRecord> toUpdate = new ArrayList<>();

        for (VenueBookingSlotTemplate template : templates) {
            if (existingTemplateIds.contains(template.getBookingSlotTemplateId())) {
                // 已有记录 - 更新状态和操作人信息
                VenueBookingSlotRecord record = existingRecords.stream()
                        .filter(r -> r.getSlotTemplateId().equals(template.getBookingSlotTemplateId()))
                        .findFirst()
                        .orElse(null);
                if (record != null) {
                    record.setStatus(BookingSlotStatus.LOCKED_IN.getValue());
                    record.setOperatorId(userId);
                    record.setOperatorSource(OperatorSourceEnum.USER);
                    toUpdate.add(record);
                    templateToRecordMap.put(template.getBookingSlotTemplateId(), record);
                }
            } else {
                // 新记录 - 创建并设置操作人信息
                VenueBookingSlotRecord record = new VenueBookingSlotRecord();
                record.setSlotTemplateId(template.getBookingSlotTemplateId());
                record.setBookingDate(bookingDate.atStartOfDay());
                record.setStatus(BookingSlotStatus.LOCKED_IN.getValue());
                record.setOperatorId(userId);
                record.setOperatorSource(OperatorSourceEnum.USER);
                toInsert.add(record);
                templateToRecordMap.put(template.getBookingSlotTemplateId(), record);
            }
        }

        // 批量insert
        if (!toInsert.isEmpty()) {
            slotRecordService.saveBatch(toInsert);
        }

        // 批量update - 使用原子性更新，只有当前状态为AVAILABLE时才能更新
        // 如果返回0表示槽位已被其他用户占用（超卖防护）
        int successUpdateCount = 0;
        for (VenueBookingSlotRecord record : toUpdate) {
            int affectedRows = slotRecordMapper.updateStatusIfAvailable(
                    record.getBookingSlotRecordId(),
                    BookingSlotStatus.LOCKED_IN.getValue(),
                    userId
            );
            if (affectedRows == 0) {
                // 该槽位已被其他用户占用，这不应该发生（因为有Redis锁）
                log.error("【事务内】[lockLock不应该出现这样的错误]槽位已被其他用户占用 - recordId: {}, slotTemplateId: {}",
                        record.getBookingSlotRecordId(), record.getSlotTemplateId());
                return null;  // 返回null表示占用失败
            }
            successUpdateCount++;
        }

        // 按照 templates 的顺序返回 records，确保顺序正确
        List<VenueBookingSlotRecord> records = templates.stream()
                .map(template -> templateToRecordMap.get(template.getBookingSlotTemplateId()))
                .collect(Collectors.toList());

        log.info("【事务内】槽位占用完成 - userId: {}, 槽位数: {}, 新增: {}, 更新: {} (成功: {}), recordIds: {}",
                userId, templates.size(), toInsert.size(), toUpdate.size(), successUpdateCount,
                records.stream().map(VenueBookingSlotRecord::getBookingSlotRecordId).toArray());

        return records;
    }
}
