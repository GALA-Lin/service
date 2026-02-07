package com.unlimited.sports.globox.venue.admin.service.impl;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.VenueCode;
import com.unlimited.sports.globox.merchant.mapper.*;
import com.unlimited.sports.globox.model.merchant.entity.*;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplatePeriod;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivitySlotLock;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueExtraChargeTemplate;
import com.unlimited.sports.globox.model.venue.enums.BookingSlotStatus;
import com.unlimited.sports.globox.model.venue.enums.VenueActivityStatusEnum;
import com.unlimited.sports.globox.venue.admin.dto.CreateVenueInitDto;
import com.unlimited.sports.globox.venue.admin.dto.CreateActivityDto;
import com.unlimited.sports.globox.venue.admin.service.IVenueInitService;
import com.unlimited.sports.globox.venue.admin.util.SlotTemplateGenerator;
import com.unlimited.sports.globox.venue.admin.vo.VenueInitResultVo;
import com.unlimited.sports.globox.venue.mapper.*;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplateMapper;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplatePeriodMapper;
import com.unlimited.sports.globox.venue.service.IVenueFacilityRelationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 场馆初始化服务实现
 */
@Slf4j
@Service
public class VenueInitServiceImpl implements IVenueInitService {

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private CourtMapper courtMapper;

    @Autowired
    private VenuePriceTemplateMapper venuePriceTemplateMapper;

    @Autowired
    private VenuePriceTemplatePeriodMapper venuePriceTemplatePeriodMapper;

    @Autowired
    private MerchantVenueBusinessHoursMapper venueBusinessHoursMapper;

    @Autowired
    private VenueBookingSlotTemplateMapper slotTemplateMapper;

    @Autowired
    private IVenueFacilityRelationService venueFacilityRelationService;

    @Autowired
    private VenueThirdPartyConfigMapper venueThirdPartyConfigMapper;

    @Autowired
    private VenueActivityMapper venueActivityMapper;

    @Autowired
    private VenueActivitySlotLockMapper venueActivitySlotLockMapper;

    @Autowired
    private VenueBookingSlotRecordMapper venueBookingSlotRecordMapper;

    @Autowired
    private VenueExtraChargeTemplateMapper venueExtraChargeTemplateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VenueInitResultVo createVenue(Long merchantId, CreateVenueInitDto dto) {
        log.info("开始创建场馆：merchantId={}, venueName={}", merchantId, dto.getVenueBasicInfo().getName());
        // 1. 获取图片URL列表
        List<String> imageUrls = dto.getImageUrls() != null ? dto.getImageUrls() : new ArrayList<>();

        // 2. 创建场馆
        Venue venue = createVenueRecord(merchantId, dto, imageUrls);
        Long venueId = venue.getVenueId();
        log.info("场馆创建成功：venueId={}", venueId);

        // 3. 创建价格模板和时段（需要在创建场地之前，以便绑定到场地）
        PriceTemplateBindResult priceTemplateResult = createPriceTemplatesForCourts(merchantId, dto);
        log.info("价格模板创建成功：templateIds={}", priceTemplateResult.getTemplateIds());

        // 4. 创建场地（绑定价格模板ID）
        List<Court> courts = createCourts(venueId, dto.getCourts(), priceTemplateResult);
        List<Long> courtIds = courts.stream().map(Court::getCourtId).collect(Collectors.toList());
        log.info("场地创建成功：courtIds={}", courtIds);

        // 5. 构建场地索引到实际ID的映射（用于处理extraCharges的applicableCourtIds）
        Map<Integer, Long> courtIndexToIdMap = new HashMap<>();
        for (int i = 0; i < courts.size(); i++) {
            courtIndexToIdMap.put(i, courts.get(i).getCourtId());
        }

        // 6. 创建营业时间
        int businessHourCount = createBusinessHours(venueId, dto.getBusinessHours());
        log.info("营业时间配置创建成功：共{}条", businessHourCount);

        // 7. 为所有场地生成槽位模板（根据营业时间）
        // 从营业时间配置中获取REGULAR类型的营业时间
        LocalTime openTime = LocalTime.of(0, 0);
        LocalTime closeTime = LocalTime.of(23, 59, 59);
        for (CreateVenueInitDto.BusinessHourConfigDto config : dto.getBusinessHours()) {
            if (config.getRuleType() == 1) {  // REGULAR类型
                openTime = LocalTime.parse(config.getOpenTime());
                // 处理24:00特殊情况
                String closeTimeStr = config.getCloseTime();
                closeTime = "24:00".equals(closeTimeStr) ? LocalTime.of(23, 59, 59) : LocalTime.parse(closeTimeStr);
                break;
            }
        }
        int totalSlots = createSlotTemplates(courtIds, openTime, closeTime);
        log.info("槽位模板创建成功：共{}个", totalSlots);

        // 8. 创建便利设施关系
        int facilityCount = createFacilities(venueId, dto.getFacilities());
        log.info("便利设施创建成功：共{}条", facilityCount);

        // 9. 如果是Away球场，创建第三方平台配置
        if (dto.getVenueType() != null && dto.getVenueType() == 2) {
            if (dto.getThirdPartyConfig() == null) {
                throw new GloboxApplicationException("Away球场必须提供第三方平台配置");
            }
            createThirdPartyConfig(venueId, dto.getThirdPartyConfig());
            log.info("第三方平台配置创建成功：venueId={}", venueId);
        }

        // 10. 创建额外费用配置（需要将applicableCourtIndices转换为实际courtIds）
        int extraChargeCount = createExtraCharges(venueId, dto.getExtraCharges(), courtIndexToIdMap);
        log.info("额外费用配置创建成功：共{}条", extraChargeCount);

        // 返回结果
        return VenueInitResultVo.builder()
                .venueId(venueId)
                .venueName(venue.getName())
                .courtIds(courtIds)
                .courtCount(courts.size())
                .priceTemplateId(priceTemplateResult.getDefaultTemplateId())
                .imageUrls(imageUrls)
                .totalSlotTemplates(totalSlots)
                .businessHourCount(businessHourCount)
                .facilityCount(facilityCount)
                .message("场馆初始化成功")
                .build();

    }

    /**
     * 创建场馆记录
     */
    private Venue createVenueRecord(Long merchantId, CreateVenueInitDto dto,
                                    List<String> imageUrls) {
        CreateVenueInitDto.VenueBasicInfoDto basicInfo = dto.getVenueBasicInfo();

        Venue venue = new Venue();
        venue.setMerchantId(merchantId);
        venue.setName(basicInfo.getName());
        venue.setAddress(basicInfo.getAddress());
        venue.setRegion(basicInfo.getRegion());
        venue.setLatitude(basicInfo.getLatitude());
        venue.setLongitude(basicInfo.getLongitude());
        venue.setPhone(basicInfo.getPhone());
        venue.setDescription(basicInfo.getDescription());
        venue.setMaxAdvanceDays(basicInfo.getMaxAdvanceDays());
        venue.setSlotVisibilityTime(LocalTime.parse(basicInfo.getSlotVisibilityTime()));
        venue.setVenueType(dto.getVenueType() != null ? dto.getVenueType() : 1);  // 使用DTO中的venueType，默认为1（自有场馆）
        venue.setStatus(1);  // 正常状态
        venue.setAvgRating(null);  // 初始无评分
        venue.setRatingCount(0);  // 初始评分数为0
        venue.setImageUrls(String.join(";", imageUrls));  // 使用;分隔多个URL

        venueMapper.insert(venue);
        return venue;
    }

    /**
     * 价格模板绑定结果
     */
    @lombok.Data
    @lombok.Builder
    private static class PriceTemplateBindResult {
        private Long defaultTemplateId;
        private List<Long> templateIds;
        private Map<Integer, Long> courtIndexToTemplateIdMap;
    }

    /**
     * 为场地创建价格模板
     * 当前实现：创建一个统一的价格模板，所有场地共用
     */
    private PriceTemplateBindResult createPriceTemplatesForCourts(Long merchantId, CreateVenueInitDto dto) {
        CreateVenueInitDto.PriceConfigDto priceConfig = dto.getPriceConfig();
        List<CreateVenueInitDto.CourtBasicInfoDto> courtDtos = dto.getCourts();

        // 检查是否有场地级别的价格配置
        boolean hasCourtLevelPriceConfig = courtDtos.stream()
                .anyMatch(court -> court.getPriceConfig() != null);

        Map<Integer, Long> courtIndexToTemplateIdMap = new HashMap<>();
        List<Long> templateIds = new ArrayList<>();

        if (hasCourtLevelPriceConfig) {
            // 场地级别价格配置：每个场地可以有自己的价格模板,由于一键插入提前拿不到场地id,这里使用下标
            for (int i = 0; i < courtDtos.size(); i++) {
                CreateVenueInitDto.CourtBasicInfoDto courtDto = courtDtos.get(i);
                CreateVenueInitDto.PriceConfigDto courtPriceConfig = courtDto.getPriceConfig();
                if (courtPriceConfig == null) {
                    // 如果场地没有配置价格，使用全局价格配置
                    courtPriceConfig = priceConfig;
                }
                if (courtPriceConfig == null) {
                    throw new GloboxApplicationException("场地 " + courtDto.getName() + " 未配置价格模板");
                }
                Long templateId = createPriceTemplate(merchantId, courtPriceConfig);
                courtIndexToTemplateIdMap.put(i, templateId);
                templateIds.add(templateId);
            }
        } else {
            // 统一价格配置：所有场地共用一个价格模板
            Long templateId = createPriceTemplate(merchantId, priceConfig);
            templateIds.add(templateId);
            for (int i = 0; i < courtDtos.size(); i++) {
                courtIndexToTemplateIdMap.put(i, templateId);
            }
        }

        return PriceTemplateBindResult.builder()
                .defaultTemplateId(templateIds.get(0))
                .templateIds(templateIds)
                .courtIndexToTemplateIdMap(courtIndexToTemplateIdMap)
                .build();
    }

    /**
     * 创建场地（绑定价格模板ID）
     */
    private List<Court> createCourts(Long venueId, List<CreateVenueInitDto.CourtBasicInfoDto> courtDtos,
                                      PriceTemplateBindResult priceTemplateResult) {
        List<Court> courts = new ArrayList<>();

        for (int i = 0; i < courtDtos.size(); i++) {
            CreateVenueInitDto.CourtBasicInfoDto dto = courtDtos.get(i);
            Long templateId = priceTemplateResult.getCourtIndexToTemplateIdMap().get(i);

            Court court = new Court();
            court.setVenueId(venueId);
            court.setName(dto.getName());
            court.setGroundType(dto.getGroundType());
            court.setCourtType(dto.getCourtType());
            court.setThirdPartyCourtId(dto.getThirdPartyCourtId());
            court.setTemplateId(templateId);  // 绑定价格模板ID
            court.setStatus(1);  // 开放状态
            courtMapper.insert(court);
            courts.add(court);
        }

        return courts;
    }

    /**
     * 创建价格模板和时段
     */
    private Long createPriceTemplate(Long merchantId, CreateVenueInitDto.PriceConfigDto priceConfig) {
        // 创建价格模板
        VenuePriceTemplate template = new VenuePriceTemplate();
        template.setMerchantId(merchantId);
        template.setTemplateName(priceConfig.getTemplateName());
        template.setIsDefault(false);
        template.setIsEnabled(true);
        venuePriceTemplateMapper.insert(template);

        Long templateId = template.getTemplateId();

        // 创建价格时段
        for (int i = 0; i < priceConfig.getPeriods().size(); i++) {
            CreateVenueInitDto.PricePeriodDto periodDto = priceConfig.getPeriods().get(i);

            VenuePriceTemplatePeriod period = new VenuePriceTemplatePeriod();
            period.setTemplateId(templateId);
            period.setStartTime(LocalTime.parse(periodDto.getStartTime()));
            // 处理24:00特殊情况
            String endTimeStr = periodDto.getEndTime();
            period.setEndTime("24:00".equals(endTimeStr) ? LocalTime.of(23, 59, 59) : LocalTime.parse(endTimeStr));
            period.setWeekdayPrice(periodDto.getWeekdayPrice());
            period.setWeekendPrice(periodDto.getWeekendPrice());
            period.setHolidayPrice(periodDto.getHolidayPrice());
            period.setIsEnabled(true);

            venuePriceTemplatePeriodMapper.insert(period);
        }

        return templateId;
    }

    /**
     * 创建营业时间
     */
    private int createBusinessHours(Long venueId, List<CreateVenueInitDto.BusinessHourConfigDto> configs) {
        int count = 0;

        for (CreateVenueInitDto.BusinessHourConfigDto config : configs) {
            VenueBusinessHours businessHour = new VenueBusinessHours();
            businessHour.setVenueId(venueId);
            businessHour.setRuleType(config.getRuleType());

            // 设置优先级：CLOSED_DATE > SPECIAL_DATE > REGULAR
            if (config.getRuleType() == 3) {  // CLOSED_DATE
                businessHour.setPriority(100);
            } else if (config.getRuleType() == 2) {  // SPECIAL_DATE
                businessHour.setPriority(10);
            } else {  // REGULAR
                businessHour.setPriority(1);
            }

            // REGULAR类型的dayOfWeek固定为0（表示每天应用）
            if (config.getRuleType() == 1) {
                businessHour.setDayOfWeek(0);  // 0表示每天
            }

            // 只有SPECIAL_DATE和CLOSED_DATE需要effectiveDate
            if (config.getRuleType() == 2 || config.getRuleType() == 3) {
                businessHour.setEffectiveDate(config.getEffectiveDate());
            }

            // CLOSED_DATE类型不需要时间
            if (config.getRuleType() != 3) {
                businessHour.setOpenTime(LocalTime.parse(config.getOpenTime()));
                // 处理24:00特殊情况
                String closeTimeStr = config.getCloseTime();
                businessHour.setCloseTime("24:00".equals(closeTimeStr) ? LocalTime.of(23, 59, 59) : LocalTime.parse(closeTimeStr));
            }

            businessHour.setRemark(config.getRemark());

            venueBusinessHoursMapper.insert(businessHour);
            count++;
        }

        return count;
    }

    /**
     * 为所有场地创建槽位模板（根据营业时间生成）
     */
    private int createSlotTemplates(List<Long> courtIds, LocalTime openTime, LocalTime closeTime) {
        List<VenueBookingSlotTemplate> allSlots = SlotTemplateGenerator.generateSlotsForMultipleCourts(courtIds, openTime, closeTime);

        for (VenueBookingSlotTemplate slot : allSlots) {
            slotTemplateMapper.insert(slot);
        }

        return allSlots.size();
    }

    /**
     * 创建便利设施关系
     */
    private int createFacilities(Long venueId, List<Integer> facilities) {
        return venueFacilityRelationService.batchCreateFacilities(venueId, facilities);
    }

    /**
     * 创建第三方平台配置（Away球场专用）
     */
    private void createThirdPartyConfig(Long venueId, CreateVenueInitDto.ThirdPartyConfigDto configDto) {
        VenueThirdPartyConfig config = VenueThirdPartyConfig.builder()
                .venueId(venueId)
                .thirdPartyPlatformId(configDto.getThirdPartyPlatformId())
                .thirdPartyVenueId(configDto.getThirdPartyVenueId())
                .username(configDto.getUsername())
                .password(configDto.getPassword())
                .apiUrl(configDto.getApiUrl())
                .extraConfig(configDto.getExtraConfig())  // 保存额外配置
                .status(1)  // 启用
                .build();

        venueThirdPartyConfigMapper.insert(config);
    }

    /**
     * 创建额外费用配置
     * @param venueId 场馆ID
     * @param extraCharges 额外费用配置列表
     * @param courtIndexToIdMap 场地索引到实际ID的映射（用于转换applicableCourtIndices）
     */
    private int createExtraCharges(Long venueId, List<CreateVenueInitDto.ExtraChargeConfigDto> extraCharges,
                                    Map<Integer, Long> courtIndexToIdMap) {
        if (extraCharges == null || extraCharges.isEmpty()) {
            return 0;
        }

        List<VenueExtraChargeTemplate> charges = extraCharges.stream()
                .map(chargeDto -> {
                    VenueExtraChargeTemplate charge = new VenueExtraChargeTemplate();
                    charge.setVenueId(venueId);
                    charge.setChargeName(chargeDto.getChargeName());
                    charge.setChargeType(chargeDto.getChargeType());
                    charge.setChargeLevel(chargeDto.getChargeLevel());
                    charge.setChargeMode(chargeDto.getChargeMode());
                    charge.setUnitAmount(chargeDto.getUnitAmount());

                    // 处理applicableCourtIds：支持两种方式
                    // 1. applicableCourtIndices - 使用场地索引（0-based），在创建时转换为实际ID
                    // 2. applicableCourtIds - 直接使用场地ID（用于已知ID的情况）
                    List<Long> actualCourtIds = null;
                    if (chargeDto.getApplicableCourtIndices() != null && !chargeDto.getApplicableCourtIndices().isEmpty()) {
                        // 将索引转换为实际的courtId
                        actualCourtIds = chargeDto.getApplicableCourtIndices().stream()
                                .filter(index -> courtIndexToIdMap.containsKey(index))
                                .map(courtIndexToIdMap::get)
                                .collect(Collectors.toList());
                        log.info("额外费用 {} 的applicableCourtIndices {} 转换为 courtIds {}",
                                chargeDto.getChargeName(), chargeDto.getApplicableCourtIndices(), actualCourtIds);
                    } else if (chargeDto.getApplicableCourtIds() != null && !chargeDto.getApplicableCourtIds().isEmpty()) {
                        actualCourtIds = chargeDto.getApplicableCourtIds();
                    }
                    charge.setApplicableCourtIds(actualCourtIds);

                    charge.setApplicableDays(chargeDto.getApplicableDays() != null ? chargeDto.getApplicableDays() : 0);
                    charge.setDescription(chargeDto.getDescription());
                    charge.setIsEnabled(chargeDto.getIsEnabled() != null ? chargeDto.getIsEnabled() : 1);
                    charge.setIsDefault(chargeDto.getIsDefault() != null ? chargeDto.getIsDefault() : 0);
                    return charge;
                })
                .toList();

        charges.forEach(venueExtraChargeTemplateMapper::insert);
        return charges.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createActivity(CreateActivityDto dto) {
        log.info("开始创建活动：venueId={}, courtId={}, activityName={}, activityDate={}, startTime={}, endTime={}",
                dto.getVenueId(), dto.getCourtId(), dto.getActivityName(), dto.getActivityDate(),
                dto.getStartTime(), dto.getEndTime());

        // 验证时间格式（只能是整点或半点）
        validateTimeFormat(dto.getStartTime(), dto.getEndTime());

        // 查询该时间段内需要锁定的所有槽位模板
        List<VenueBookingSlotTemplate> templates = querySlotTemplates(dto.getCourtId(), dto.getStartTime(), dto.getEndTime());
        if (templates.isEmpty()) {
            throw new GloboxApplicationException("该时间段不存在槽位模板");
        }

        // 验证这些槽位在指定日期是否被占用
        List<Long> templateIds = templates.stream().map(VenueBookingSlotTemplate::getBookingSlotTemplateId).collect(Collectors.toList());
        validateSlotsAvailability(templateIds, dto.getActivityDate());

        // 创建活动记录
        VenueActivity activity = VenueActivity.builder()
                .venueId(dto.getVenueId())
                .courtId(dto.getCourtId())
                .activityTypeId(dto.getActivityTypeId())
                .activityTypeDesc(dto.getActivityTypeDesc())
                .activityName(dto.getActivityName())
                .activityDate(dto.getActivityDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .maxParticipants(dto.getMaxParticipants())
                .currentParticipants(0)
                .unitPrice(dto.getUnitPrice())
                .description(dto.getDescription())
                .registrationDeadline(null)
                .organizerId(dto.getOrganizerId())
                .organizerType(dto.getOrganizerType())
                .minNtrpLevel(dto.getMinNtrpLevel())
                .status(VenueActivityStatusEnum.NORMAL.getValue())
                .build();

        venueActivityMapper.insert(activity);
        Long activityId = activity.getActivityId();
        log.info("活动创建成功，activityId={}", activityId);

        // 为每个槽位模板创建锁定记录
        for (Long templateId : templateIds) {
            VenueActivitySlotLock lock = VenueActivitySlotLock.builder()
                    .activityId(activityId)
                    .slotTemplateId(templateId)
                    .bookingDate(dto.getActivityDate())
                    .build();
            venueActivitySlotLockMapper.insert(lock);
        }
        log.info("活动槽位锁定成功，已锁定{}个槽位", templateIds.size());

        return activityId;
    }

    /**
     * 验证时间格式（只能是整点或半点）
     */
    private void validateTimeFormat(LocalTime startTime, LocalTime endTime) {
        int startMinute = startTime.getMinute();
        int endMinute = endTime.getMinute();

        if (startMinute != 0 && startMinute != 30) {
            throw new GloboxApplicationException("开始时间只能是整点或半点（:00或:30）");
        }

        if (endMinute != 0 && endMinute != 30) {
            throw new GloboxApplicationException("结束时间只能是整点或半点（:00或:30）");
        }

        if (startTime.compareTo(endTime) >= 0) {
            throw new GloboxApplicationException("结束时间必须晚于开始时间");
        }

        // 验证时间差至少为30分钟
        int durationMinutes = (endTime.getHour() * 60 + endTime.getMinute()) - (startTime.getHour() * 60 + startTime.getMinute());
        if (durationMinutes < 30) {
            throw new GloboxApplicationException("活动时间至少需要30分钟");
        }
    }

    /**
     * 查询指定时间段内的所有槽位模板
     */
    private List<VenueBookingSlotTemplate> querySlotTemplates(Long courtId, LocalTime startTime, LocalTime endTime) {
        return slotTemplateMapper.selectList(
                new LambdaQueryWrapper<VenueBookingSlotTemplate>()
                        .eq(VenueBookingSlotTemplate::getCourtId, courtId)
                        .ge(VenueBookingSlotTemplate::getStartTime, startTime)
                        .lt(VenueBookingSlotTemplate::getEndTime, endTime)
                        .orderByAsc(VenueBookingSlotTemplate::getStartTime)
        );
    }

    /**
     * 验证指定日期的槽位是否可用
     */
    private void validateSlotsAvailability(List<Long> templateIds, java.time.LocalDate bookingDate) {
        java.time.LocalDateTime startOfDay = bookingDate.atStartOfDay();
        java.time.LocalDateTime endOfDay = bookingDate.plusDays(1).atStartOfDay();

        // 查询这些槽位在指定日期是否被占用
        List<VenueBookingSlotRecord> records = venueBookingSlotRecordMapper.selectList(
                new LambdaQueryWrapper<VenueBookingSlotRecord>()
                        .in(VenueBookingSlotRecord::getSlotTemplateId, templateIds)
                        .ge(VenueBookingSlotRecord::getBookingDate, startOfDay)
                        .lt(VenueBookingSlotRecord::getBookingDate, endOfDay)
                        .in(VenueBookingSlotRecord::getStatus, BookingSlotStatus.LOCKED_IN.getValue(), BookingSlotStatus.EXPIRED.getValue())
        );

        if (!records.isEmpty()) {
            throw new GloboxApplicationException(VenueCode.SLOT_OCCUPIED);
        }
    }
}
