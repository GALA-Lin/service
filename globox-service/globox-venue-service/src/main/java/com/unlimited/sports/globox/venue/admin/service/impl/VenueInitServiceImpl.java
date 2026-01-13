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
import com.unlimited.sports.globox.model.venue.enums.BookingSlotStatus;
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

        // 3. 创建场地
        List<Court> courts = createCourts(venueId, dto.getCourts());
        List<Long> courtIds = courts.stream().map(Court::getCourtId).collect(Collectors.toList());
        log.info("场地创建成功：courtIds={}", courtIds);

        // 4. 创建价格模板和时段
        Long priceTemplateId = createPriceTemplate(merchantId, dto.getPriceConfig());
        log.info("价格模板创建成功：templateId={}", priceTemplateId);

        // 5. 更新场馆的模板ID
        venue.setTemplateId(priceTemplateId);
        venueMapper.updateById(venue);

        // 6. 创建营业时间
        int businessHourCount = createBusinessHours(venueId, dto.getBusinessHours());
        log.info("营业时间配置创建成功：共{}条", businessHourCount);

        // 7. 为所有场地生成槽位模板
        int totalSlots = createSlotTemplates(courtIds);
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

        // 返回结果
        return VenueInitResultVo.builder()
                .venueId(venueId)
                .venueName(venue.getName())
                .courtIds(courtIds)
                .courtCount(courts.size())
                .priceTemplateId(priceTemplateId)
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

        // 计算最低价格
        BigDecimal minPrice = dto.getPriceConfig().getPeriods().stream()
                .map(CreateVenueInitDto.PricePeriodDto::getWeekdayPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

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
        venue.setMinPrice(minPrice);
        venue.setStatus(1);  // 正常状态
        venue.setAvgRating(null);  // 初始无评分
        venue.setRatingCount(0);  // 初始评分数为0
        venue.setImageUrls(String.join(";", imageUrls));  // 使用;分隔多个URL

        venueMapper.insert(venue);
        return venue;
    }

    /**
     * 创建场地
     */
    private List<Court> createCourts(Long venueId, List<CreateVenueInitDto.CourtBasicInfoDto> courtDtos) {
        List<Court> courts = new ArrayList<>();

        for (CreateVenueInitDto.CourtBasicInfoDto dto : courtDtos) {
            Court court = new Court();
            court.setVenueId(venueId);
            court.setName(dto.getName());
            court.setGroundType(dto.getGroundType());
            court.setCourtType(dto.getCourtType());
            court.setThirdPartyCourtId(dto.getThirdPartyCourtId());  // 设置第三方场地ID（Away球场专用）
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
            period.setEndTime(LocalTime.parse(periodDto.getEndTime()));
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
                businessHour.setCloseTime(LocalTime.parse(config.getCloseTime()));
            }

            businessHour.setRemark(config.getRemark());

            venueBusinessHoursMapper.insert(businessHour);
            count++;
        }

        return count;
    }

    /**
     * 为所有场地创建槽位模板（每个场地48个30分钟的槽位）
     */
    private int createSlotTemplates(List<Long> courtIds) {
        List<VenueBookingSlotTemplate> allSlots = SlotTemplateGenerator.generateSlotsForMultipleCourts(courtIds);

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
                .status(1)  // 启用
                .build();

        venueThirdPartyConfigMapper.insert(config);
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
