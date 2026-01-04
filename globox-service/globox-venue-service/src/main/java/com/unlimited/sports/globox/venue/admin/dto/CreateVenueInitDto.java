package com.unlimited.sports.globox.venue.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 一键创建场馆初始化DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "一键创建场馆初始化请求")
public class CreateVenueInitDto {

    /**
     * 商家ID
     */
    @NotNull(message = "商家ID不能为空")
    @Schema(description = "商家ID")
    private Long merchantId;

    /**
     * 场馆基本信息
     */
    @Valid
    @NotNull(message = "场馆基本信息不能为空")
    @Schema(description = "场馆基本信息")
    private VenueBasicInfoDto venueBasicInfo;

    /**
     * 场地列表
     */
    @Valid
    @NotEmpty(message = "至少需要一个场地")
    @Size(max = 50, message = "场地数量不能超过50个")
    @Schema(description = "场地列表")
    private List<CourtBasicInfoDto> courts;

    /**
     * 营业时间配置
     */
    @Valid
    @NotEmpty(message = "至少需要一个营业时间配置")
    @Schema(description = "营业时间配置")
    private List<BusinessHourConfigDto> businessHours;

    /**
     * 价格配置
     */
    @Valid
    @NotNull(message = "价格配置不能为空")
    @Schema(description = "价格配置")
    private PriceConfigDto priceConfig;

    /**
     * 便利设施列表
     * 设施ID列表（对应FacilityType的value）
     * 1=停车场, 2=更衣室, 3=穿线机
     */
    @Size(max = 20, message = "设施数量不能超过20个")
    @Schema(description = "便利设施列表（设施ID）")
    private List<Integer> facilities;

    /**
     * 场馆图片URL列表
     * 通过/admin/venue/init/images/upload接口上传后获得的图片URL列表
     */
    @Size(max = 10, message = "图片数量不能超过10个")
    @Schema(description = "场馆图片URL列表（来自图片上传接口）")
    private List<String> imageUrls;

    /**
     * 场馆基本信息
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "场馆基本信息")
    public static class VenueBasicInfoDto {

        @NotBlank(message = "场馆名称不能为空")
        @Length(min = 2, max = 50, message = "场馆名称长度2-50个字符")
        @Schema(description = "场馆名称")
        private String name;

        @NotBlank(message = "场馆地址不能为空")
        @Length(min = 5, max = 100, message = "场馆地址长度5-100个字符")
        @Schema(description = "详细地址")
        private String address;

        @NotBlank(message = "所属区域不能为空")
        @Schema(description = "所属区域")
        private String region;

        @NotNull(message = "纬度不能为空")
        @DecimalMin(value = "0", message = "纬度格式不正确")
        @DecimalMax(value = "90", message = "纬度范围0-90")
        @Schema(description = "纬度")
        private BigDecimal latitude;

        @NotNull(message = "经度不能为空")
        @DecimalMin(value = "0", message = "经度格式不正确")
        @DecimalMax(value = "180", message = "经度范围0-180")
        @Schema(description = "经度")
        private BigDecimal longitude;

        @NotBlank(message = "联系电话不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号码")
        @Schema(description = "联系电话")
        private String phone;

        @Schema(description = "场馆介绍")
        private String description;

        @NotNull(message = "提前预订天数不能为空")
        @Min(value = 1, message = "最少提前1天预订")
        @Max(value = 30, message = "最多提前30天预订")
        @Schema(description = "提前多少天开放订场")
        private Integer maxAdvanceDays;

        @NotBlank(message = "开放订场时间不能为空")
        @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$", message = "时间格式必须为HH:mm:ss")
        @Schema(description = "几点开放订场")
        private String slotVisibilityTime;
    }

    /**
     * 场地基本信息
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "场地基本信息")
    public static class CourtBasicInfoDto {

        @NotBlank(message = "场地名称不能为空")
        @Length(min = 1, max = 50, message = "场地名称长度1-50个字符")
        @Schema(description = "场地名称")
        private String name;

        @NotNull(message = "地面类型不能为空")
        @Min(value = 1, message = "地面类型范围1-4")
        @Max(value = 4, message = "地面类型范围1-4")
        @Schema(description = "地面类型：1=硬地，2=红土，3=草地，4=其他")
        private Integer groundType;

        @NotNull(message = "场地类型不能为空")
        @Min(value = 1, message = "场地类型范围1-4")
        @Max(value = 4, message = "场地类型范围1-4")
        @Schema(description = "场地类型：1=室内，2=室外，3=风雨场，4=半封闭")
        private Integer courtType;
    }

    /**
     * 营业时间配置
     */
    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "营业时间配置")
    public static class BusinessHourConfigDto {

        @NotNull(message = "规则类型不能为空")
        @Min(value = 1, message = "规则类型必须为1-3")
        @Max(value = 3, message = "规则类型必须为1-3")
        @Schema(description = "规则类型：1=REGULAR(每天重复)，2=SPECIAL_DATE(特定日期)，3=CLOSED_DATE(关闭日期)")
        private Integer ruleType;

        @Builder.Default
        @Schema(description = "星期几：0=每天，仅REGULAR类型使用（默认为0，表示每天应用）")
        private Integer dayOfWeek = 0;

        @Schema(description = "特定日期，SPECIAL_DATE和CLOSED_DATE类型需要")
        private LocalDate effectiveDate;

        @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$", message = "时间格式必须为HH:mm:ss")
        @Schema(description = "开门时间，CLOSED_DATE类型不需要")
        private String openTime;

        @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$", message = "时间格式必须为HH:mm:ss")
        @Schema(description = "关门时间，CLOSED_DATE类型不需要")
        private String closeTime;

        @Schema(description = "备注")
        private String remark;
    }

    /**
     * 价格配置
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "价格配置")
    public static class PriceConfigDto {

        @NotBlank(message = "价格模板名称不能为空")
        @Length(min = 2, max = 50, message = "模板名称长度2-50个字符")
        @Schema(description = "价格模板名称")
        private String templateName;

        @Valid
        @NotEmpty(message = "至少需要一个价格时段")
        @Size(max = 10, message = "价格时段不能超过10个")
        @Schema(description = "价格时段列表")
        private List<PricePeriodDto> periods;
    }

    /**
     * 价格时段
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "价格时段")
    public static class PricePeriodDto {

        @NotBlank(message = "开始时间不能为空")
        @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$", message = "时间格式必须为HH:mm:ss")
        @Schema(description = "开始时间")
        private String startTime;

        @NotBlank(message = "结束时间不能为空")
        @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$", message = "时间格式必须为HH:mm:ss")
        @Schema(description = "结束时间")
        private String endTime;

        @NotNull(message = "工作日价格不能为空")
        @DecimalMin(value = "0.01", message = "工作日价格必须大于0")
        @Digits(integer = 6, fraction = 2, message = "价格格式不正确")
        @Schema(description = "工作日价格")
        private BigDecimal weekdayPrice;

        @NotNull(message = "周末价格不能为空")
        @DecimalMin(value = "0.01", message = "周末价格必须大于0")
        @Digits(integer = 6, fraction = 2, message = "价格格式不正确")
        @Schema(description = "周末价格")
        private BigDecimal weekendPrice;

        @NotNull(message = "节假日价格不能为空")
        @DecimalMin(value = "0.01", message = "节假日价格必须大于0")
        @Digits(integer = 6, fraction = 2, message = "价格格式不正确")
        @Schema(description = "节假日价格")
        private BigDecimal holidayPrice;
    }
}
