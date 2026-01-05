package com.unlimited.sports.globox.merchant.service.impl;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        courtMapper.updateById(court);
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

    // TODO 与其他服务抽离VO转化 ETA 2026/01/15

    private CourtVo convertToVO(Court court) {
        GroundTypeEnum groundType = GroundTypeEnum.getByCode(court.getGroundType());
        CourtTypeEnum courtType = CourtTypeEnum.getByCode(court.getCourtType());

        return CourtVo.builder()
                .courtId(court.getCourtId())
                .venueId(court.getVenueId())
                .name(court.getName())
                .groundType(court.getGroundType())
                .groundTypeName(Optional.ofNullable(groundType).map(GroundTypeEnum::getName).orElse("未知"))
                .courtType(court.getCourtType())
                .courtTypeName(Optional.ofNullable(courtType).map(CourtTypeEnum::getName).orElse("未知"))
                .statusName(court.getStatus() == 1 ? "开放" : "不开放")
                .createdAt(court.getCreatedAt())
                .build();
    }
}