package com.unlimited.sports.globox.dubbo.merchant.dto;

import com.unlimited.sports.globox.model.merchant.entity.Court;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单服务获取 venue 信息 - result dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueSnapshotResultDto implements Serializable {

    @NotNull
    private Long id;

    @NotNull
    private String name;

    @NotNull
    private String phone;

    @NotNull
    private String region;

    @NotNull
    private String address;

    @NotNull
    private String coverImage;

    /**
     * 单位 km
     */
    @NotNull
    private BigDecimal distance;

    @NotNull
    private List<String> facilities;

    @NotNull
    private List<CourtSnapshotDto> courtSnapshotDtos;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourtSnapshotDto implements Serializable {

        @NotNull
        private Long id;

        @NotNull
        private String name;

        @NotNull
        private Integer groundType;

        @NotNull
        private Integer courtType;
    }
}
