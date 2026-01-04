package com.unlimited.sports.globox.venue.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 场馆初始化结果VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "场馆初始化结果")
public class VenueInitResultVo {

    @Schema(description = "场馆ID")
    private Long venueId;

    @Schema(description = "场馆名称")
    private String venueName;

    @Schema(description = "场地ID列表")
    private List<Long> courtIds;

    @Schema(description = "创建的场地数量")
    private Integer courtCount;

    @Schema(description = "价格模板ID")
    private Long priceTemplateId;

    @Schema(description = "上传的图片URL列表")
    private List<String> imageUrls;

    @Schema(description = "生成的槽位模板总数")
    private Integer totalSlotTemplates;

    @Schema(description = "创建的营业时间配置数")
    private Integer businessHourCount;

    @Schema(description = "创建的便利设施数")
    private Integer facilityCount;

    @Schema(description = "成功消息")
    private String message;
}
