package com.unlimited.sports.globox.merchant.service.impl;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.model.merchant.dto.CourtBatchCreateDto;
import com.unlimited.sports.globox.model.merchant.enums.CourtTypeEnum;
import com.unlimited.sports.globox.model.merchant.enums.GroundTypeEnum;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.service.CourtManagementService;
import com.unlimited.sports.globox.model.merchant.dto.CourtCreateDto;
import com.unlimited.sports.globox.model.merchant.dto.CourtUpdateDto;
import com.unlimited.sports.globox.model.merchant.vo.CourtVo;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.vo.MerchantVenueBasicInfo;
import com.unlimited.sports.globox.model.merchant.vo.MerchantVenueDetailVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Linsen Hu
 * @since 2025/12/22 14:08
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourtManagementServiceImpl implements CourtManagementService {

    private final CourtMapper courtMapper;
    private final VenueMapper venueMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourtVo createCourt(Long merchantId, CourtCreateDto createDTO) {
        // 验证场馆归属
        Venue venue = venueMapper.selectById(createDTO.getVenueId());
        if (venue == null || !venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该场馆");
        }

        // 创建场地
        Court court = Court.builder()
                .venueId(createDTO.getVenueId())
                .name(createDTO.getName())
                .groundType(createDTO.getGroundType())
                .courtType(createDTO.getCourtType())
                .status(1)  // 默认开放
                .build();

        courtMapper.insert(court);

        // 重新查询以获取数据库生成的字段（created_at, updated_at等）
        Court insertedCourt = courtMapper.selectById(court.getCourtId());

        log.info("创建场地成功，场地ID：{}，场馆ID：{}", insertedCourt.getCourtId(), createDTO.getVenueId());
        return convertToVO(insertedCourt);
    }

    /**
     * @param merchantId
     * @param batchDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CourtVo> batchCreateCourts(Long merchantId, CourtBatchCreateDto batchDTO) {
        Long venueId = batchDTO.getVenueId();

        // 1. 验证场馆归属 (确保安全，防止越权)
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null || !venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该场馆");
        }

        // 2. 遍历并创建
        List<CourtVo> resultVos = batchDTO.getCourts().stream().map(item -> {
            Court court = Court.builder()
                    .venueId(venueId)
                    .name(item.getName())
                    .groundType(item.getGroundType())
                    .courtType(item.getCourtType())
                    .status(1) // 默认开放
                    .build();

            courtMapper.insert(court);

            // 重新查询以获取数据库生成的字段（如 ID, createdAt）
            Court inserted = courtMapper.selectById(court.getCourtId());
            return convertToVO(inserted);
        }).collect(Collectors.toList());

        log.info("批量创建场地成功，场馆ID：{}，创建数量：{}", venueId, resultVos.size());
        return resultVos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourtVo updateCourt(Long merchantId, CourtUpdateDto updateDTO) {
        // 查询场地
        Court court = courtMapper.selectById(updateDTO.getCourtId());
        if (court == null) {
            throw new GloboxApplicationException("场地不存在");
        }

        // 验证场馆归属
        Venue venue = venueMapper.selectById(court.getVenueId());
        if (venue == null || !venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该场地");
        }

        Court updatedCourt = court.toBuilder()
                .name(updateDTO.getName() != null ? updateDTO.getName() : court.getName())
                .groundType(updateDTO.getGroundType() != null ? updateDTO.getGroundType() : court.getGroundType())
                .courtType(updateDTO.getCourtType() != null ? updateDTO.getCourtType() : court.getCourtType())
                .status(updateDTO.getStatus() != null ? updateDTO.getStatus() : court.getStatus())
                .build();

        courtMapper.updateById(updatedCourt);
        Court latestCourt = courtMapper.selectById(updateDTO.getCourtId());

        log.info("更新场地成功，场地ID：{}", updateDTO.getCourtId());
        return convertToVO(latestCourt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourt(Long merchantId, Long courtId) {
        // 查询场地
        Court court = courtMapper.selectById(courtId);
        if (court == null) {
            throw new GloboxApplicationException("场地不存在");
        }

        // 验证场馆归属
        Venue venue = venueMapper.selectById(court.getVenueId());
        if (venue == null || !venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该场地");
        }

        // TODO: 检查是否有未完成的订单 ETA 2026/01/15
        // TODO: 逻辑删除 ETA 2026/01/01
        // 删除场地
        courtMapper.deleteById(courtId);

        log.info("删除场地成功，场地ID：{}", courtId);
    }

    @Override
    public List<CourtVo> listCourtsByVenue(Long merchantId, Long venueId) {
        // 验证场馆归属
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null || !venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权访问该场馆");
        }

        // 查询场地列表
        List<Court> courts = courtMapper.selectByVenueId(venueId);

        return courts.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourtVo toggleCourtStatus(Long merchantId, Long courtId, Integer status) {
        // 查询场地
        Court court = courtMapper.selectById(courtId);
        if (court == null) {
            throw new GloboxApplicationException("场地不存在");
        }

        // 验证场馆归属
        Venue venue = venueMapper.selectById(court.getVenueId());
        if (venue == null || !venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该场地");
        }

        // 更新状态
        court.setStatus(status);
        courtMapper.updateById(court);

        log.info("切换场地状态成功，场地ID：{}，状态：{}", courtId, status);
        return convertToVO(court);
    }

    /**
     * @param merchantId
     * @return
     */
    @Override
    public List<MerchantVenueBasicInfo> getVenuesByMerchantId(Long merchantId) {
        log.info("查询商家场馆详细信息 - merchantId: {}", merchantId);

        // 查询商家所有场馆
        List<Venue> venues = venueMapper.selectVenuesByMerchantId(merchantId);

        if (venues == null || venues.isEmpty()) {
            log.warn("商家没有场馆 - merchantId: {}", merchantId);
            return Collections.emptyList();
        }

        // 转换为 VO
        return venues.stream()
                .map(this::convertToVenueBasicVo)
                .collect(Collectors.toList());
    }

    @Override
    public List<MerchantVenueDetailVo> getVenuesWithCourts(Long merchantId) {
        log.info("[场馆场地嵌套查询] merchantId: {}", merchantId);

        // 1. 获取该商家下所有场馆
        List<Venue> venues = venueMapper.selectVenuesByMerchantId(merchantId);
        if (venues == null || venues.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 转换场馆基本信息并建立映射
        return venues.stream().map(venue -> {
            // 转换场馆基本信息 (复用原有逻辑)
            MerchantVenueBasicInfo basicInfo = convertToVenueBasicVo(venue);

            boolean isVenueClosed = venue.getStatus() != 1;

            // 3. 查询该场馆下的所有场地并转换为 VO (复用原有逻辑 convertToVO)
            List<Court> courtEntities = courtMapper.selectByVenueId(venue.getVenueId());
            List<CourtVo> courtVos = courtEntities.stream()
                    .map(court -> this.convertToVoForList(court, isVenueClosed))
                    .collect(Collectors.toList());

            // 4. 组装成嵌套对象
            return MerchantVenueDetailVo.builder()
                    .venueId(basicInfo.getVenueId())
                    .venueName(basicInfo.getName())
                    .address(basicInfo.getAddress())
                    .region(basicInfo.getRegion())
                    .imageUrls(basicInfo.getImageUrls())
                    .status(basicInfo.getStatus())
                    .statusDesc(basicInfo.getStatus() == 1 ? "正常营业" : "暂停营业")
                    .courts(courtVos)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 转换场馆实体为 VO
     */
    private MerchantVenueBasicInfo convertToVenueBasicVo(Venue venue) {
        // 解析图片URL
        List<String> imageUrlList = parseImageUrls(venue.getImageUrls());

        // 获取状态描述
        String statusDesc = venue.getStatus() == 1 ? "正常营业" : "暂停营业";

        return MerchantVenueBasicInfo.builder()
                .venueId(venue.getVenueId())
                .name(venue.getName())
                .address(venue.getAddress())
                .region(venue.getRegion())
                .imageUrls(imageUrlList)
                .status(venue.getStatus())
                .statusDesc(statusDesc)
                .build();
    }

    /**
     * 解析图片URL字符串为列表
     * @param imageUrls 以分号分隔的URL字符串
     * @return URL列表
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


    private CourtVo convertToVO(Court court) {
        GroundTypeEnum groundType = GroundTypeEnum.getByCode(court.getGroundType());
        CourtTypeEnum courtType = CourtTypeEnum.getByCode(court.getCourtType());

        return CourtVo.builder()
                .courtId(court.getCourtId())
                .venueId(court.getVenueId())
                .courtName(court.getName())
                .groundType(court.getGroundType())
                .groundTypeName(Optional.ofNullable(groundType).map(GroundTypeEnum::getName).orElse("未知"))
                .courtType(court.getCourtType())
                .courtTypeName(Optional.ofNullable(courtType).map(CourtTypeEnum::getName).orElse("未知"))
                .status(court.getStatus())
                .statusName(court.getStatus() == 1 ? "开放" : "不开放")
                .createdAt(court.getCreatedAt())
                .build();
    }

    private CourtVo convertToVoForList(Court court, boolean isVenueClosed) {
        GroundTypeEnum groundType = GroundTypeEnum.getByCode(court.getGroundType());
        CourtTypeEnum courtType = CourtTypeEnum.getByCode(court.getCourtType());

        Integer finalCourtStatus = isVenueClosed ? 0 : court.getStatus();
        String finalStatusName = isVenueClosed ? "不开放" : (court.getStatus() == 1 ? "开放" : "不开放");

        return CourtVo.builder()
                .courtId(court.getCourtId())
                .venueId(court.getVenueId())
                .courtName(court.getName())
                .groundType(court.getGroundType())
                .groundTypeName(Optional.ofNullable(groundType).map(GroundTypeEnum::getName).orElse("未知"))
                .courtType(court.getCourtType())
                .courtTypeName(Optional.ofNullable(courtType).map(CourtTypeEnum::getName).orElse("未知"))
                .status(finalCourtStatus)  // 使用最终状态
                .statusName(finalStatusName)  // 使用最终状态名称
                .createdAt(court.getCreatedAt())
                .build();
    }
}