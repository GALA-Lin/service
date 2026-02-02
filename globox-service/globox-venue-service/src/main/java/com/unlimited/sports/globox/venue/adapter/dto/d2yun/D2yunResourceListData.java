package com.unlimited.sports.globox.venue.adapter.dto.d2yun;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * D2yun资源列表响应数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class D2yunResourceListData {

    /**
     * 场地设置信息
     */
    private D2yunSetting setting;

    /**
     * 场地列表
     */
    private List<D2yunSpace> spaces;

    /**
     * 资源列表（二维数组，每个场地一个资源列表）
     */
    private List<List<D2yunResource>> resources;
}
