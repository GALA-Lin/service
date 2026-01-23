package com.unlimited.sports.globox.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.MerchantVenueBusinessHoursMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.service.VenueManagementService;
import com.unlimited.sports.globox.model.merchant.dto.VenueBusinessHoursDto;
import com.unlimited.sports.globox.model.merchant.dto.VenueCreateDto;
import com.unlimited.sports.globox.model.merchant.dto.VenueUpdateDto;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.entity.VenueBusinessHours;
import com.unlimited.sports.globox.model.merchant.enums.BusinessHourRuleTypeEnum;
import com.unlimited.sports.globox.model.merchant.vo.MerchantVenueBasicInfo;
import com.unlimited.sports.globox.model.merchant.vo.VenueInfoVo;
import com.unlimited.sports.globox.model.merchant.vo.VenueBusinessHoursVo;
import com.unlimited.sports.globox.venue.service.impl.VenueBusinessHoursService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 场馆管理Service实现类
 * @author Linsen Hu
 * @since 2026-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VenueManagementServiceImpl implements VenueManagementService {

    private final VenueMapper venueMapper;
    private final MerchantVenueBusinessHoursMapper businessHoursMapper;
    private final CourtMapper courtMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantVenueBasicInfo createVenue(Long merchantId, VenueCreateDto dto) {
        log.info("创建场馆 - merchantId: {}, venueName: {}", merchantId, dto.getName());

        // 1. 构建场馆实体
        Venue venue = buildVenueFromDto(dto);
        venue.setMerchantId(merchantId);
        venue.setVenueType(1); // 默认为自有场馆
        venue.setStatus(1); // 默认正常营业

        // 2. 插入场馆基本信息
        venueMapper.insert(venue);
        log.info("场馆基本信息创建成功 - venueId: {}", venue.getVenueId());

//        // 3. 创建营业时间规则
//        createBusinessHours(venue.getVenueId(), dto.getBusinessHours());
//        log.info("场馆营业时间创建成功 - venueId: {}, 规则数: {}",
//                venue.getVenueId(), dto.getBusinessHours().size());

        // 4. 返回基本信息
        return convertToVenueBasicVo(venue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantVenueBasicInfo updateVenue(Long merchantId, VenueUpdateDto dto) {
        log.info("更新场馆 - merchantId: {}, venueId: {}", merchantId, dto.getVenueId());

        // 1. 验证场馆归属
        Venue venue = validateVenueOwnership(dto.getVenueId(), merchantId);

        // 2. 更新基本信息
        updateVenueBasicInfo(venue, dto);
        venueMapper.updateById(venue);
        log.info("场馆基本信息更新成功 - venueId: {}", venue.getVenueId());

        // 3. 更新营业时间（如果提供）
        if (dto.getBusinessHours() != null && !dto.getBusinessHours().isEmpty()) {
            updateBusinessHours(venue.getVenueId(), dto.getBusinessHours());
            log.info("场馆营业时间更新成功 - venueId: {}, 规则数: {}",
                    venue.getVenueId(), dto.getBusinessHours().size());
        } else if (Boolean.TRUE.equals(dto.getClearBusinessHours())) {
            // 清空营业时间
            deleteBusinessHours(venue.getVenueId());
            log.info("场馆营业时间已清空 - venueId: {}", venue.getVenueId());
        }

        // 4. 返回基本信息
        return convertToVenueBasicVo(venue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVenue(Long merchantId, Long venueId) {
        log.info("删除场馆 - merchantId: {}, venueId: {}", merchantId, venueId);

        // 1. 验证场馆归属
        validateVenueOwnership(venueId, merchantId);

        // TODO: 检查是否有场地和未完成的订单

        // 2. 删除营业时间规则
        deleteBusinessHours(venueId);

        // 3. 删除场馆（物理删除，建议改为逻辑删除）
        venueMapper.deleteById(venueId);

        log.info("场馆删除成功 - venueId: {}", venueId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantVenueBasicInfo toggleVenueStatus(Long merchantId, Long venueId, Integer status) {
        log.info("切换场馆状态 - merchantId: {}, venueId: {}, status: {}",
                merchantId, venueId, status);

        // 1. 验证场馆归属
        Venue venue = validateVenueOwnership(venueId, merchantId);

        // 2. 更新状态
        venue.setStatus(status);
        venueMapper.updateById(venue);

        if (status == 0) {
            courtMapper.disableCourtsByVenueId(venueId);
            log.info("场馆禁用，同步禁用场地 - venueId: {}", venueId);
        }

        log.info("场馆状态切换成功 - venueId: {}, status: {}", venueId, status);

        // 3. 返回基本信息
        return convertToVenueBasicVo(venue);
    }

    @Override
    public VenueInfoVo getVenueDetail(Long merchantId, Long venueId) {
        log.info("查询场馆详情 - merchantId: {}, venueId: {}", merchantId, venueId);

        // 1. 验证场馆归属
        Venue venue = validateVenueOwnership(venueId, merchantId);

        // 2. 查询营业时间
        List<VenueBusinessHours> businessHours = businessHoursMapper.selectByVenueId(venueId);

        // 3. 转换为VO
        return convertToVenueDetailVo(venue, businessHours);
    }

    // ==================== 私有方法 ====================

    /**
     * 验证场馆归属
     */
    private Venue validateVenueOwnership(Long venueId, Long merchantId) {
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null) {
            throw new GloboxApplicationException("场馆不存在");
        }
        if (!venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该场馆");
        }
        return venue;
    }

    /**
     * 从DTO构建场馆实体
     */
    private Venue buildVenueFromDto(VenueCreateDto dto) {
        Venue venue = new Venue();
        venue.setName(dto.getName());
        venue.setAddress(dto.getAddress());
        venue.setRegion(dto.getRegion());
        venue.setLatitude(dto.getLatitude());
        venue.setLongitude(dto.getLongitude());
        venue.setPhone(dto.getPhone());
        venue.setDescription(dto.getDescription());
        venue.setMaxAdvanceDays(dto.getMaxAdvanceDays());
        venue.setSlotVisibilityTime(dto.getSlotVisibilityTime());

        // 处理设施标签
        if (dto.getFacilities() != null && !dto.getFacilities().isEmpty()) {
            venue.setFacilities(String.join(";", dto.getFacilities()));
        }

        // 处理图片URL
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            venue.setImageUrls(String.join(";", dto.getImageUrls()));
        }

        return venue;
    }

    /**
     * 更新场馆基本信息
     */
    private void updateVenueBasicInfo(Venue venue, VenueUpdateDto dto) {
        if (dto.getName() != null) venue.setName(dto.getName());
        if (dto.getAddress() != null) venue.setAddress(dto.getAddress());
        if (dto.getRegion() != null) venue.setRegion(dto.getRegion());
        if (dto.getLatitude() != null) venue.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) venue.setLongitude(dto.getLongitude());
        if (dto.getPhone() != null) venue.setPhone(dto.getPhone());
        if (dto.getDescription() != null) venue.setDescription(dto.getDescription());
        if (dto.getMaxAdvanceDays() != null) venue.setMaxAdvanceDays(dto.getMaxAdvanceDays());
        if (dto.getSlotVisibilityTime() != null) venue.setSlotVisibilityTime(dto.getSlotVisibilityTime());
        if (dto.getStatus() != null) venue.setStatus(dto.getStatus());

        // 更新设施标签
        if (dto.getFacilities() != null) {
            venue.setFacilities(dto.getFacilities().isEmpty() ?
                    null : String.join(";", dto.getFacilities()));
        }

        // 更新图片URL
        updateImageUrls(venue, dto);
    }

    /**
     * 更新图片URL
     */
    private void updateImageUrls(Venue venue, VenueUpdateDto dto) {
        List<String> finalImageUrls = new ArrayList<>();

        // 如果不清空原有图片，保留旧图片
        if (!Boolean.TRUE.equals(dto.getClearImages()) && venue.getImageUrls() != null) {
            finalImageUrls.addAll(parseImageUrls(venue.getImageUrls()));
        }

        // 添加新上传的图片
        if (dto.getImageUrls() != null) {
            finalImageUrls.addAll(dto.getImageUrls());
        }

        // 更新图片字段
        if (!finalImageUrls.isEmpty()) {
            venue.setImageUrls(String.join(";", finalImageUrls));
        } else if (Boolean.TRUE.equals(dto.getClearImages())) {
            venue.setImageUrls(null);
        }
    }

    /**
     * 创建营业时间规则
     */
    private void createBusinessHours(Long venueId, List<VenueBusinessHoursDto> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return;
        }

        // 验证营业时间规则
        validateBusinessHours(dtoList);

        // 转换并插入
        for (VenueBusinessHoursDto dto : dtoList) {
            VenueBusinessHours entity = convertToBusinessHoursEntity(venueId, dto);
            businessHoursMapper.insert(entity);
        }
    }

    /**
     * 更新营业时间规则（完全覆盖）
     */
    private void updateBusinessHours(Long venueId, List<VenueBusinessHoursDto> dtoList) {
        // 1. 删除原有规则
        deleteBusinessHours(venueId);

        // 2. 创建新规则
        createBusinessHours(venueId, dtoList);
    }

    /**
     * 删除营业时间规则
     */
    private void deleteBusinessHours(Long venueId) {
        businessHoursMapper.delete(
                new LambdaQueryWrapper<VenueBusinessHours>()
                        .eq(VenueBusinessHours::getVenueId, venueId)
        );
    }

    /**
     * 验证营业时间规则
     */
    private void validateBusinessHours(List<VenueBusinessHoursDto> dtoList) {
        for (VenueBusinessHoursDto dto : dtoList) {
            BusinessHourRuleTypeEnum ruleType = BusinessHourRuleTypeEnum.fromCode(dto.getRuleType());
            if (ruleType == null) {
                throw new GloboxApplicationException("无效的规则类型: " + dto.getRuleType());
            }

            // 验证字段完整性
            switch (ruleType) {
                case REGULAR:
                    if (dto.getDayOfWeek() == null) {
                        throw new GloboxApplicationException("常规规则必须指定星期几");
                    }
                    if (dto.getOpenTime() == null || dto.getCloseTime() == null) {
                        throw new GloboxApplicationException("常规规则必须指定营业时间");
                    }
                    validateTimeRange(dto.getOpenTime(), dto.getCloseTime());
                    break;

                case SPECIAL_DATE:
                    if (dto.getEffectiveDate() == null) {
                        throw new GloboxApplicationException("特殊日期规则必须指定日期");
                    }
                    if (dto.getOpenTime() == null || dto.getCloseTime() == null) {
                        throw new GloboxApplicationException("特殊日期规则必须指定营业时间");
                    }
                    validateTimeRange(dto.getOpenTime(), dto.getCloseTime());
                    break;

                case CLOSED_DATE:
                    if (dto.getEffectiveDate() == null) {
                        throw new GloboxApplicationException("关闭日期规则必须指定日期");
                    }
                    break;
            }
        }
    }

    /**
     * 验证时间范围
     */
    private void validateTimeRange(LocalTime openTime, LocalTime closeTime) {
        if (openTime.isAfter(closeTime) || openTime.equals(closeTime)) {
            throw new GloboxApplicationException("营业开始时间必须早于结束时间");
        }
    }

    /**
     * 转换DTO为营业时间实体
     */
    private VenueBusinessHours convertToBusinessHoursEntity(Long venueId, VenueBusinessHoursDto dto) {
        VenueBusinessHours entity = new VenueBusinessHours();
        entity.setVenueId(venueId);
        entity.setRuleType(dto.getRuleType());
        entity.setDayOfWeek(dto.getDayOfWeek());
        entity.setEffectiveDate(dto.getEffectiveDate());
        entity.setOpenTime(dto.getOpenTime());
        entity.setCloseTime(dto.getCloseTime());
        entity.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        entity.setRemark(dto.getRemark());
        return entity;
    }

    /**
     * 转换场馆实体为基本信息VO
     */
    private MerchantVenueBasicInfo convertToVenueBasicVo(Venue venue) {
        return MerchantVenueBasicInfo.builder()
                .venueId(venue.getVenueId())
                .name(venue.getName())
                .address(venue.getAddress())
                .region(venue.getRegion())
                .imageUrls(parseImageUrls(venue.getImageUrls()))
                .status(venue.getStatus())
                .statusDesc(venue.getStatus() == 1 ? "正常营业" : "暂停营业")
                .build();
    }

    /**
     * 转换场馆实体为详情VO
     */
    private VenueInfoVo convertToVenueDetailVo(Venue venue,
                                                         List<VenueBusinessHours> businessHours) {
        return VenueInfoVo.builder()
                .venueId(venue.getVenueId())
                .venueName(venue.getName())
                .address(venue.getAddress())
                .region(venue.getRegion())
                .latitude(venue.getLatitude())
                .longitude(venue.getLongitude())
                .phone(venue.getPhone())
                .description(venue.getDescription())
                .maxAdvanceDays(venue.getMaxAdvanceDays())
                .slotVisibilityTime(venue.getSlotVisibilityTime())
                .imageUrls(parseImageUrls(venue.getImageUrls()))
                .facilities(parseFacilities(venue.getFacilities()))
                .status(venue.getStatus())
                .statusDesc(venue.getStatus() == 1 ? "正常营业" : "暂停营业")
                .businessHours(convertToBusinessHoursVoList(businessHours))
                .build();
    }

    /**
     * 转换营业时间实体列表为VO列表
     */
    private List<VenueBusinessHoursVo> convertToBusinessHoursVoList(List<VenueBusinessHours> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return Collections.emptyList();
        }

        return entityList.stream()
                .map(this::convertToBusinessHoursVo)
                .collect(Collectors.toList());
    }

    /**
     * 转换营业时间实体为VO
     */
    private VenueBusinessHoursVo convertToBusinessHoursVo(VenueBusinessHours entity) {
        VenueBusinessHoursVo vo = new VenueBusinessHoursVo();
        vo.setBusinessHourId(entity.getBusinessHourId());
        vo.setVenueId(entity.getVenueId());
        vo.setRuleType(entity.getRuleType());
        vo.setRuleTypeName(getRuleTypeName(entity.getRuleType()));
        vo.setDayOfWeek(entity.getDayOfWeek());
        vo.setDayOfWeekName(getDayOfWeekName(entity.getDayOfWeek()));
        vo.setEffectiveDate(entity.getEffectiveDate());
        vo.setOpenTime(entity.getOpenTime());
        vo.setCloseTime(entity.getCloseTime());
        vo.setPriority(entity.getPriority());
        vo.setRemark(entity.getRemark());
        return vo;
    }

    /**
     * 获取规则类型名称
     */
    private String getRuleTypeName(Integer ruleType) {
        BusinessHourRuleTypeEnum type = BusinessHourRuleTypeEnum.fromCode(ruleType);
        return type != null ? type.getName() : "未知";
    }

    /**
     * 获取星期名称
     */
    private String getDayOfWeekName(Integer dayOfWeek) {
        if (dayOfWeek == null) return null;
        return switch (dayOfWeek) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "未知";
        };
    }

    /**
     * 解析图片URL字符串为列表
     */
    private List<String> parseImageUrls(String imageUrls) {
        if (imageUrls == null || imageUrls.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(imageUrls.split(";"))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 解析设施标签字符串为列表
     */
    private List<String> parseFacilities(String facilities) {
        if (facilities == null || facilities.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(facilities.split(";"))
                .map(String::trim)
                .filter(f -> !f.isEmpty())
                .collect(Collectors.toList());
    }
}