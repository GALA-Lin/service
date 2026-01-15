package com.unlimited.sports.globox.dubbo.user;

import com.unlimited.sports.globox.common.enums.RegionCityEnum;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.user.dto.RegionDto;

import java.util.List;

public interface RegionDubboService {

    /**
     * 按 code 查询行政区划信息
     *
     * @param code 行政区划 code
     * @return 行政区划信息
     */
    RpcResult<RegionDto> getRegionByCode(String code);

    /**
     * 查询某个城市下的所有区/县（level=3）
     *
     * @param regionCity 城市枚举
     * @return 区县列表
     */
    RpcResult<List<RegionDto>> listDistrictsByCity(RegionCityEnum regionCity);


}
