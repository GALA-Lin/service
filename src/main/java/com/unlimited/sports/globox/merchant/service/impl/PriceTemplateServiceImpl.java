package com.unlimited.sports.globox.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.*;
import com.unlimited.sports.globox.merchant.service.PriceTemplateService;
import com.unlimited.sports.globox.model.merchant.dto.*;
import com.unlimited.sports.globox.model.merchant.entity.*;
import com.unlimited.sports.globox.model.merchant.vo.*;

import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplatePeriod;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplateMapper;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplatePeriodMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 价格模板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceTemplateServiceImpl implements PriceTemplateService {

    private final VenuePriceTemplateMapper priceTemplateMapper;
    private final VenuePriceTemplatePeriodMapper priceTemplatePeriodMapper;
    private final VenueMapper venueMapper;
    private final BookingSlotMapper bookingSlotMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PriceTemplateVo createPriceTemplate(Long merchantId, CreatePriceTemplateDto dto) {
        // 验证时段是否有重叠
        validatePeriods(dto.getPeriods());

        // 如果设置为默认模板，先将其他模板的默认状态取消
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearDefaultTemplates(merchantId);
        }

        // 创建价格模板
        VenuePriceTemplate template = new VenuePriceTemplate();
        template.setMerchantId(merchantId);
        template.setTemplateName(dto.getTemplateName());
        template.setIsDefault(dto.getIsDefault());
        template.setIsEnabled(true);

        priceTemplateMapper.insert(template);

        // 创建价格时段
        for (PriceTemplatePeriodDto periodDto : dto.getPeriods()) {
            VenuePriceTemplatePeriod period = new VenuePriceTemplatePeriod();
            period.setTemplateId(template.getTemplateId());
            period.setStartTime(periodDto.getStartTime());
            period.setEndTime(periodDto.getEndTime());
            period.setWeekdayPrice(periodDto.getWeekdayPrice());
            period.setWeekendPrice(periodDto.getWeekendPrice());
            period.setHolidayPrice(periodDto.getHolidayPrice());
            period.setIsEnabled(periodDto.getIsEnabled());

            priceTemplatePeriodMapper.insert(period);
        }

        log.info("创建价格模板成功，商家ID：{}，模板ID：{}，模板名称：{}",
                merchantId, template.getTemplateId(), dto.getTemplateName());

        return getPriceTemplate(merchantId, template.getTemplateId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PriceTemplateVo updatePriceTemplate(Long merchantId, UpdatePriceTemplateDto dto) {
        // 验证模板归属
        VenuePriceTemplate template = priceTemplateMapper.selectById(dto.getTemplateId());
        if (template == null) {
            throw new GloboxApplicationException("价格模板不存在");
        }
        if (!template.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该价格模板");
        }

        // 更新模板基本信息
        if (StringUtils.hasText(dto.getTemplateName())) {
            template.setTemplateName(dto.getTemplateName());
        }
        if (dto.getIsEnabled() != null) {
            template.setIsEnabled(dto.getIsEnabled());
        }

        // 如果设置为默认模板，先将其他模板的默认状态取消
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearDefaultTemplates(merchantId);
            template.setIsDefault(true);
        } else if (Boolean.FALSE.equals(dto.getIsDefault())) {
            template.setIsDefault(false);
        }

        priceTemplateMapper.updateById(template);

        // 如果提供了新的时段列表，更新时段
        if (dto.getPeriods() != null && !dto.getPeriods().isEmpty()) {
            validatePeriods(dto.getPeriods());

            // 逻辑删除原有时段（设置为不启用）
            LambdaQueryWrapper<VenuePriceTemplatePeriod> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(VenuePriceTemplatePeriod::getTemplateId, dto.getTemplateId());
            List<VenuePriceTemplatePeriod> oldPeriods = priceTemplatePeriodMapper.selectList(wrapper);

            for (VenuePriceTemplatePeriod oldPeriod : oldPeriods) {
                oldPeriod.setIsEnabled(false);
                priceTemplatePeriodMapper.updateById(oldPeriod);
            }

            // 插入新时段
            for (PriceTemplatePeriodDto periodDto : dto.getPeriods()) {
                VenuePriceTemplatePeriod period = new VenuePriceTemplatePeriod();
                period.setTemplateId(template.getTemplateId());
                period.setStartTime(periodDto.getStartTime());
                period.setEndTime(periodDto.getEndTime());
                period.setWeekdayPrice(periodDto.getWeekdayPrice());
                period.setWeekendPrice(periodDto.getWeekendPrice());
                period.setHolidayPrice(periodDto.getHolidayPrice());
                period.setIsEnabled(periodDto.getIsEnabled());

                priceTemplatePeriodMapper.insert(period);
            }
        }

        log.info("更新价格模板成功，商家ID：{}，模板ID：{}", merchantId, dto.getTemplateId());

        return getPriceTemplate(merchantId, dto.getTemplateId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePriceTemplate(Long merchantId, Long templateId) {
        // 验证模板归属
        VenuePriceTemplate template = priceTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new GloboxApplicationException("价格模板不存在");
        }
        if (!template.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该价格模板");
        }

        // 检查是否有场馆正在使用该模板
        LambdaQueryWrapper<Venue> venueWrapper = new LambdaQueryWrapper<>();
        venueWrapper.eq(Venue::getTemplateId, templateId);
        Long venueCount = venueMapper.selectCount(venueWrapper);

        if (venueCount > 0) {
            throw new GloboxApplicationException(
                    String.format("该价格模板正在被 %d 个场馆使用，无法删除", venueCount));
        }

        // 逻辑删除价格时段（设置为不启用）
        LambdaQueryWrapper<VenuePriceTemplatePeriod> periodWrapper = new LambdaQueryWrapper<>();
        periodWrapper.eq(VenuePriceTemplatePeriod::getTemplateId, templateId);
        List<VenuePriceTemplatePeriod> periods = priceTemplatePeriodMapper.selectList(periodWrapper);

        for (VenuePriceTemplatePeriod period : periods) {
            period.setIsEnabled(false);
            priceTemplatePeriodMapper.updateById(period);
        }

        // 逻辑删除价格模板（设置为不启用）
        template.setIsEnabled(false);
        priceTemplateMapper.updateById(template);

        log.info("删除价格模板成功（逻辑删除），商家ID：{}，模板ID：{}", merchantId, templateId);
    }

    @Override
    public PriceTemplateVo getPriceTemplate(Long merchantId, Long templateId) {
        // 验证模板归属
        VenuePriceTemplate template = priceTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new GloboxApplicationException("价格模板不存在");
        }
        if (!template.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权访问该价格模板");
        }

        // 查询价格时段
        List<VenuePriceTemplatePeriod> periods = priceTemplatePeriodMapper.selectByTemplateId(templateId);

        // 查询使用该模板的场馆数量
        LambdaQueryWrapper<Venue> venueWrapper = new LambdaQueryWrapper<>();
        venueWrapper.eq(Venue::getTemplateId, templateId);
        Long venueCount = venueMapper.selectCount(venueWrapper);

        // 转换为VO
        return convertToVo(template, periods, venueCount.intValue());
    }

    @Override
    public Page<PriceTemplateSimpleVo> queryPriceTemplates(Long merchantId, QueryPriceTemplateDto dto) {
        Page<VenuePriceTemplate> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        LambdaQueryWrapper<VenuePriceTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VenuePriceTemplate::getMerchantId, merchantId);

        if (StringUtils.hasText(dto.getTemplateName())) {
            wrapper.like(VenuePriceTemplate::getTemplateName, dto.getTemplateName());
        }
        if (dto.getIsDefault() != null) {
            wrapper.eq(VenuePriceTemplate::getIsDefault, dto.getIsDefault());
        }
        if (dto.getIsEnabled() != null) {
            wrapper.eq(VenuePriceTemplate::getIsEnabled, dto.getIsEnabled());
        }

        wrapper.orderByDesc(VenuePriceTemplate::getIsDefault)
                .orderByDesc(VenuePriceTemplate::getCreatedAt);

        Page<VenuePriceTemplate> templatePage = priceTemplateMapper.selectPage(page, wrapper);

        // 转换为简要VO
        Page<PriceTemplateSimpleVo> resultPage = new Page<>(dto.getPageNum(), dto.getPageSize());
        resultPage.setTotal(templatePage.getTotal());
        resultPage.setRecords(templatePage.getRecords().stream()
                .map(this::convertToSimpleVo)
                .collect(Collectors.toList()));

        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindPriceTemplate(Long merchantId, BindPriceTemplateDto dto) {
        // 验证场馆归属
        Venue venue = venueMapper.selectById(dto.getVenueId());
        if (venue == null) {
            throw new GloboxApplicationException("场馆不存在");
        }
        if (!venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该场馆");
        }

        // 验证模板归属
        VenuePriceTemplate template = priceTemplateMapper.selectById(dto.getTemplateId());
        if (template == null) {
            throw new GloboxApplicationException("价格模板不存在");
        }
        if (!template.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权使用该价格模板");
        }

        // 更新场馆的价格模板
        venue.setTemplateId(dto.getTemplateId());
        venueMapper.updateById(venue);

        // 如果需要刷新已生成的时段价格
        if (Boolean.TRUE.equals(dto.getRefreshExistingSlots())) {
            // TODO: 实现刷新未来所有未支付时段的价格
            // 这里需要调用SlotManagementService的refreshSlots方法
            log.info("刷新场馆 {} 的未来时段价格（功能待实现）", dto.getVenueId());
        }

        log.info("绑定价格模板成功，场馆ID：{}，模板ID：{}", dto.getVenueId(), dto.getTemplateId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultTemplate(Long merchantId, Long templateId) {
        // 验证模板归属
        VenuePriceTemplate template = priceTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new GloboxApplicationException("价格模板不存在");
        }
        if (!template.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该价格模板");
        }

        // 取消其他默认模板
        clearDefaultTemplates(merchantId);

        // 设置为默认模板
        template.setIsDefault(true);
        priceTemplateMapper.updateById(template);

        log.info("设置默认价格模板成功，商家ID：{}，模板ID：{}", merchantId, templateId);
    }

    /**
     * 取消商家的所有默认模板
     */
    private void clearDefaultTemplates(Long merchantId) {
        LambdaQueryWrapper<VenuePriceTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VenuePriceTemplate::getMerchantId, merchantId)
                .eq(VenuePriceTemplate::getIsDefault, true);

        List<VenuePriceTemplate> templates = priceTemplateMapper.selectList(wrapper);
        for (VenuePriceTemplate template : templates) {
            template.setIsDefault(false);
            priceTemplateMapper.updateById(template);
        }
    }

    /**
     * 验证时段是否有重叠
     */
    private void validatePeriods(List<PriceTemplatePeriodDto> periods) {
        for (int i = 0; i < periods.size(); i++) {
            PriceTemplatePeriodDto period1 = periods.get(i);

            // 验证单个时段的有效性
            if (period1.getStartTime().isAfter(period1.getEndTime()) ||
                    period1.getStartTime().equals(period1.getEndTime())) {
                throw new GloboxApplicationException(
                        String.format("时段 %s - %s 无效，开始时间必须早于结束时间",
                                period1.getStartTime(), period1.getEndTime()));
            }

            // 检查与其他时段是否重叠
            for (int j = i + 1; j < periods.size(); j++) {
                PriceTemplatePeriodDto period2 = periods.get(j);

                boolean overlap = !(period1.getEndTime().isBefore(period2.getStartTime()) ||
                        period1.getEndTime().equals(period2.getStartTime()) ||
                        period2.getEndTime().isBefore(period1.getStartTime()) ||
                        period2.getEndTime().equals(period1.getStartTime()));

                if (overlap) {
                    throw new GloboxApplicationException(
                            String.format("时段 %s - %s 与时段 %s - %s 存在重叠",
                                    period1.getStartTime(), period1.getEndTime(),
                                    period2.getStartTime(), period2.getEndTime()));
                }
            }
        }
    }

    /**
     * 转换为详细VO
     */
    private PriceTemplateVo convertToVo(VenuePriceTemplate template,
                                        List<VenuePriceTemplatePeriod> periods,
                                        Integer venueCount) {
        // 转换时段列表
        List<PriceTemplatePeriodVo> periodVos = periods.stream()
                .map(this::convertPeriodToVo)
                .collect(Collectors.toList());

        // 使用 Builder 构建 PriceTemplateVo
        return PriceTemplateVo.builder()
                .templateId(template.getTemplateId())
                .merchantId(template.getMerchantId())
                .templateName(template.getTemplateName())
                .isDefault(template.getIsDefault())
                .isEnabled(template.getIsEnabled())
                .venueCount(venueCount)
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .periods(periodVos)
                .build();
    }

    /**
     * 转换为简要VO
     */
    private PriceTemplateSimpleVo convertToSimpleVo(VenuePriceTemplate template) {
        // 查询时段信息
        List<VenuePriceTemplatePeriod> periods = priceTemplatePeriodMapper.selectByTemplateId(template.getTemplateId());
        int periodCount = periods.size();

        // 处理价格区间
        String priceRange = "";
        if (periodCount > 0) {
            BigDecimal minPrice = periods.stream()
                    .map(VenuePriceTemplatePeriod::getWeekdayPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal maxPrice = periods.stream()
                    .flatMap(p -> Stream.of(
                            p.getWeekdayPrice(),
                            p.getWeekendPrice(),
                            p.getHolidayPrice()
                    ))
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            priceRange = minPrice.intValue() + "-" + maxPrice.intValue() + "元";
        }

        // 查询使用该模板的场馆数量
        LambdaQueryWrapper<Venue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Venue::getTemplateId, template.getTemplateId());
        Long venueCount = venueMapper.selectCount(wrapper);

        // 使用Builder模式构建VO
        return PriceTemplateSimpleVo.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getTemplateName())
                .isDefault(template.getIsDefault())
                .isEnabled(template.getIsEnabled())
                .createdAt(template.getCreatedAt())
                .periodCount(periodCount)
                .priceRange(priceRange)
                .venueCount(venueCount != null ? venueCount.intValue() : 0)
                .build();
    }

    /**
     * 转换时段为VO
     */
    private PriceTemplatePeriodVo convertPeriodToVo(VenuePriceTemplatePeriod period) {
        return PriceTemplatePeriodVo.builder()
                .periodId(period.getPeriodId())
                .startTime(period.getStartTime())
                .endTime(period.getEndTime())
                .weekdayPrice(period.getWeekdayPrice())
                .weekendPrice(period.getWeekendPrice())
                .holidayPrice(period.getHolidayPrice())
                .isEnabled(period.getIsEnabled())
                .createdAt(period.getCreatedAt())
                .updatedAt(period.getUpdatedAt())
                .build();
    }
}