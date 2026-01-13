package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

/**
 * 批量创建场地DTO
 */
@Data
public class CourtBatchCreateDto {

    @NotNull(message = "场馆ID不能为空")
    private Long venueId;

    @NotEmpty(message = "场地列表不能为空")
    @Valid
    private List<CourtItemDto> courts;

    @Data
    public static class CourtItemDto {
        @NotBlank(message = "场地名称不能为空")
        @Size(max = 100, message = "场地名称长度不能超过100字符")
        private String name;

        @NotNull(message = "场地地面类型不能为空")
        private Integer groundType;

        @NotNull(message = "场地类型不能为空")
        private Integer courtType;
    }
}