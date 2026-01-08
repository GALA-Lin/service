package com.unlimited.sports.globox.venue.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 第三方平台场地槽位统一DTO
 * 用于统一不同第三方平台的返回格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdPartyCourtSlotDto {

    /**
     * 第三方平台的场地ID
     */
    private String thirdPartyCourtId;

    /**
     * 第三方平台的场地名称（可选）
     */
    private String courtName;

    /**
     * 槽位列表
     */
    private List<ThirdPartySlotDto> slots;
}
