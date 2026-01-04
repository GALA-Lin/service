package com.unlimited.sports.globox.venue.admin.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.*;
import com.unlimited.sports.globox.model.merchant.entity.*;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplatePeriod;
import com.unlimited.sports.globox.venue.admin.dto.CreateVenueInitDto;
import com.unlimited.sports.globox.venue.admin.service.IVenueInitService;
import com.unlimited.sports.globox.venue.admin.util.SlotTemplateGenerator;
import com.unlimited.sports.globox.venue.admin.vo.VenueInitResultVo;
import com.unlimited.sports.globox.venue.mapper.*;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplateMapper;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplatePeriodMapper;
import com.unlimited.sports.globox.venue.service.IVenueFacilityRelationService;
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
    private VenueBusinessHoursMapper venueBusinessHoursMapper;

    @Autowired
    private VenueBookingSlotTemplateMapper slotTemplateMapper;

    @Autowired
    private IVenueFacilityRelationService venueFacilityRelationService;

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
        venue.setVenueType(1);  // 默认为自有场馆
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
}
