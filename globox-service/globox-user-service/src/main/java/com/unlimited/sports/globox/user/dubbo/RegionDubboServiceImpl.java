package com.unlimited.sports.globox.user.dubbo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.enums.RegionCityEnum;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.user.RegionDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.RegionDto;
import com.unlimited.sports.globox.model.user.entity.Region;
import com.unlimited.sports.globox.user.mapper.RegionMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@DubboService(group = "rpc")
@RequiredArgsConstructor
public class RegionDubboServiceImpl implements RegionDubboService {

    private final RegionMapper regionMapper;

    @Override
    public RpcResult<RegionDto> getRegionByCode(String code) {
        if (code == null || code.isEmpty()) {
            return RpcResult.ok(null);
        }
        Region region = regionMapper.selectOne(
                Wrappers.<Region>lambdaQuery()
                        .eq(Region::getCode, code)
                        .eq(Region::getEnabled, 1)
        );
        if (region == null) {
            return RpcResult.ok(null);
        }
        return RpcResult.ok(RegionDto.builder()
                .code(region.getCode())
                .name(region.getName())
                .build());
    }

    @Override
    public RpcResult<List<RegionDto>> listDistrictsByCity(RegionCityEnum regionCity) {
        // TODO 缓存
        List<Region> list = regionMapper.selectList(
                Wrappers.<Region>lambdaQuery()
                        .eq(Region::getParentCode, regionCity.getCode())
                        .eq(Region::getLevel, 3)
                        .eq(Region::getEnabled, 1)
                        .orderByAsc(Region::getSortNo)
                        .orderByAsc(Region::getCode)
        );

        List<RegionDto> result = list.stream()
                .map(r -> RegionDto.builder()
                        .code(r.getCode())
                        .name(r.getName())
                        .build())
                .toList();
        return RpcResult.ok(result);
    }
}
