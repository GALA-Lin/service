package com.unlimited.sports.globox.model.venue.vo;

import com.unlimited.sports.globox.common.utils.DistanceUtils;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 场馆信息快照
 * 公共VO，用于订单预览和订单详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueSnapshotVo {

    /**
     * 场馆ID
     */
    @NotNull
    private Long id;

    /**
     * 场馆名称
     */
    @NotNull
    private String name;

    /**
     * 联系电话
     */
    @NotNull
    private String phone;

    /**
     * 所在区域
     */
    @NotNull
    private String region;

    /**
     * 详细地址
     */
    @NotNull
    private String address;

    /**
     * 封面图URL
     */
    @NotNull
    private String coverImage;

    /**
     * 用户到场馆距离（km）
     */
    @NotNull
    private BigDecimal distance;

    /**
     * 场馆设施列表
     */
    @NotNull
    private List<String> facilities;


    /**
     * 构建场馆快照信息
     * @param userLatitude 用户纬度
     * @param userLongitude 用户经度
     * @return 场馆快照信息
     */
    public static VenueSnapshotVo buildVenueSnapshotVo(
            Double userLatitude,
            Double userLongitude,
            Venue venue,
            List<String> facilities,
            String defaultVenueCoverImage ) {

        // 计算距离
        BigDecimal distance = BigDecimal.ZERO;
        if (venue.getLatitude() != null && venue.getLongitude() != null) {
            distance = DistanceUtils.calculateDistance(
                    userLatitude,
                    userLongitude,
                    venue.getLatitude().doubleValue(),
                    venue.getLongitude().doubleValue()
            );
        }
        // 获取封面图片
        String coverImage = defaultVenueCoverImage;
        if (venue.getImageUrls() != null && !venue.getImageUrls().isEmpty()) {
            String[] imageUrls = venue.getImageUrls().split(";");
            if (imageUrls.length > 0 && !imageUrls[0].trim().isEmpty()) {
                coverImage = imageUrls[0].trim();
            }
        }
        // 构建返回结果
        return VenueSnapshotVo.builder()
                .id(venue.getVenueId())
                .name(venue.getName())
                .phone(venue.getPhone())
                .region(venue.getRegion())
                .address(venue.getAddress())
                .coverImage(coverImage)
                .distance(distance)
                .facilities(facilities)
                .build();
    }


}
