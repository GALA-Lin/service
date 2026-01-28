package com.unlimited.sports.globox.model.venue.vo;

import com.unlimited.sports.globox.common.vo.SearchDocumentDto;
import com.unlimited.sports.globox.common.vo.SearchResultItem;
import com.unlimited.sports.globox.model.venue.enums.CourtType;
import com.unlimited.sports.globox.model.venue.enums.FacilityType;
import com.unlimited.sports.globox.model.venue.enums.GroundType;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Slf4j
@Data
@Builder
public class VenueItemVo {

    @NonNull
    private Long venueId;

    @NonNull
    private String name;

    @NonNull
    private String region;

    private BigDecimal distance;

    @NonNull
    private String coverImage;

    @NonNull
    private BigDecimal avgRating;

    @NonNull
    private Integer ratingCount;

    @NonNull
    private BigDecimal minPrice;

    @NonNull
    private List<Integer> courtTypes;

    @NonNull
    private List<String> courtTypesDesc;

    @NonNull
    private List<Integer> groundTypes;

    @NonNull
    private List<String> groundTypesDesc;

    @NonNull
    private List<String> facilities;

    @NonNull
    private Integer courtCount;

    @NonNull
    private BigDecimal latitude;

    @NonNull
    private BigDecimal longitude;

    /**
     * 从搜索文档DTO转换为场馆列表项
     *
     * @param searchDocDto 搜索文档DTO
     * @return 场馆搜索结果项
     */
    public  static SearchResultItem<VenueItemVo> fromSearchDocument(SearchDocumentDto searchDocDto) {
        log.info("dto:  {}",searchDocDto);
        // 转换球场类型描述
        List<String> courtTypesDesc = CourtType.getDescriptionsByValues(searchDocDto.getVenueCourtTypes());

        // 转换地面类型描述
        List<String> groundTypesDesc = GroundType.getDescriptionsByValues(searchDocDto.getVenueGroundTypes());

        // 转换设施代码为设施描述
        List<String> facilitiesDesc = FacilityType.getDescriptionsByValues(searchDocDto.getVenueFacilities());

        // 构建VenueItemVo
        VenueItemVo venueItem = VenueItemVo.builder()
                .venueId(searchDocDto.getBusinessId())
                .name(searchDocDto.getTitle())
                .region(searchDocDto.getRegion())
                .distance(searchDocDto.getDistance())
                .coverImage(searchDocDto.getCoverUrl())
                .avgRating(searchDocDto.getRating() != null ? BigDecimal.valueOf(searchDocDto.getRating()) : BigDecimal.ZERO)
                .ratingCount(searchDocDto.getRatingCount() != null ? searchDocDto.getRatingCount() : 0)
                .minPrice(searchDocDto.getPriceMin() != null ? BigDecimal.valueOf(searchDocDto.getPriceMin()) : BigDecimal.ZERO)
                .courtTypes(searchDocDto.getVenueCourtTypes() != null ? searchDocDto.getVenueCourtTypes() : Collections.emptyList())
                .courtTypesDesc(courtTypesDesc)
                .groundTypes(searchDocDto.getVenueGroundTypes() != null ? searchDocDto.getVenueGroundTypes() : Collections.emptyList())
                .groundTypesDesc(groundTypesDesc)
                .facilities(facilitiesDesc)
                .courtCount(searchDocDto.getVenueCourtCount() != null ? searchDocDto.getVenueCourtCount() : 0)
                .latitude(searchDocDto.getLatitude() != null ? BigDecimal.valueOf(searchDocDto.getLatitude()) : BigDecimal.ZERO)
                .longitude(searchDocDto.getLongitude() != null ? BigDecimal.valueOf(searchDocDto.getLongitude()) : BigDecimal.ZERO)
                .build();

        // 返回搜索结果项
        return SearchResultItem.<VenueItemVo>builder()
                .dataType(searchDocDto.getDataType())
                .data(venueItem)
                .build();
    }
}
