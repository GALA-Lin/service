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
     * 场馆类型：1=自有场馆，2=Away球场（第三方平台）
     * 默认为1
     */
    @NotNull(message = "场馆类型不能为空")
    @Min(value = 1, message = "场馆类型范围1-2")
    @Max(value = 2, message = "场馆类型范围1-2")
    @Schema(description = "场馆类型：1=自有场馆，2=Away球场（第三方平台）")
    private Integer venueType;

    /**
     * 第三方平台配置（仅当venueType=2时需要）
     */
    @Schema(description = "第三方平台配置（Away球场专用）")
    private ThirdPartyConfigDto thirdPartyConfig;

    /**
     * 额外费用配置列表（如灯光费、教练费等）
     */
    @Valid
    @Size(max = 20, message = "额外费用配置不能超过20个")
    @Schema(description = "额外费用配置列表")
    private List<ExtraChargeConfigDto> extraCharges;

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

        @Schema(description = "第三方场地ID（仅Away球场需要）")
        private String thirdPartyCourtId;
    }

    /**
     * 第三方平台配置（Away球场专用）
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "第三方平台配置")
    public static class ThirdPartyConfigDto {

        @NotNull(message = "第三方平台ID不能为空")
        @Schema(description = "第三方平台ID")
        private Long thirdPartyPlatformId;

        @NotBlank(message = "第三方场馆ID不能为空")
        @Schema(description = "第三方平台的场馆ID")
        private String thirdPartyVenueId;

        @NotBlank(message = "用户名不能为空")
        @Schema(description = "第三方平台账号")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Schema(description = "第三方平台密码")
        private String password;

        @Schema(description = "API地址（可选，为空则使用平台默认地址）")
        private String apiUrl;

        @Schema(description = "额外配置（JSON字符串，存储平台特定的配置数据，如clubId、courtProjectId等）")
        private String extraConfig;
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
        @DecimalMin(value = "0", message = "工作日价格不能为负数")
        @Digits(integer = 6, fraction = 2, message = "价格格式不正确")
        @Schema(description = "工作日价格")
        private BigDecimal weekdayPrice;

        @NotNull(message = "周末价格不能为空")
        @DecimalMin(value = "0", message = "周末价格不能为负数")
        @Digits(integer = 6, fraction = 2, message = "价格格式不正确")
        @Schema(description = "周末价格")
        private BigDecimal weekendPrice;

        @NotNull(message = "节假日价格不能为空")
        @DecimalMin(value = "0", message = "节假日价格不能为负数")
        @Digits(integer = 6, fraction = 2, message = "价格格式不正确")
        @Schema(description = "节假日价格")
        private BigDecimal holidayPrice;
    }

    /**
     * 额外费用配置
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "额外费用配置")
    public static class ExtraChargeConfigDto {

        @NotBlank(message = "费用名称不能为空")
        @Length(min = 1, max = 50, message = "费用名称长度1-50个字符")
        @Schema(description = "费用名称（商家自定义），如LED灯光费、专业教练")
        private String chargeName;

        @NotNull(message = "费用类型不能为空")
        @Min(value = 1, message = "费用类型范围1-6")
        @Max(value = 6, message = "费用类型范围1-6")
        @Schema(description = "费用类型：1=LIGHT(灯光费)，2=COACH(教练费)，3=EQUIPMENT(器材费)，4=PARKING(停车费)，5=CLEANING(清洁费)，6=OTHER(其他)")
        private Integer chargeType;

        @NotNull(message = "费用级别不能为空")
        @Min(value = 1, message = "费用级别范围1-2")
        @Max(value = 2, message = "费用级别范围1-2")
        @Schema(description = "费用级别：1=ORDER_LEVEL(订单级别)，2=ORDER_ITEM_LEVEL(订单项级别)")
        private Integer chargeLevel;

        @NotNull(message = "计费方式不能为空")
        @Min(value = 1, message = "计费方式范围1-2")
        @Max(value = 2, message = "计费方式范围1-2")
        @Schema(description = "计费方式：1=FIXED(固定金额)，2=PERCENTAGE(百分比)")
        private Integer chargeMode;

        @NotNull(message = "单位金额或比例不能为空")
        @DecimalMin(value = "0", message = "单位金额或比例不能为负数")
        @Digits(integer = 6, fraction = 2, message = "数值格式不正确")
        @Schema(description = "单位金额或比例（FIXED时为固定费用金额，PERCENTAGE时为百分比值，如5表示5%）")
        private BigDecimal unitAmount;

        @Schema(description = "适用场地ID列表（留空表示对所有场地适用）")
        private List<Long> applicableCourtIds;

        @Min(value = 0, message = "适用天数范围0-2")
        @Max(value = 2, message = "适用天数范围0-2")
        @Schema(description = "适用天数：0=ALL(所有天)，1=WEEKDAY(工作日)，2=WEEKEND(周末)，默认为0")
        private Integer applicableDays = 0;

        @Schema(description = "费用描述")
        @Length(max = 200, message = "费用描述长度不能超过200个字符")
        private String description;

        @Schema(description = "是否启用：0=否，1=是，默认为1")
        @Min(value = 0, message = "是否启用范围0-1")
        @Max(value = 1, message = "是否启用范围0-1")
        private Integer isEnabled = 1;

        @Schema(description = "是否为默认费用（用户预订时自动选中）：0=否，1=是，默认为0")
        @Min(value = 0, message = "是否为默认费用范围0-1")
        @Max(value = 1, message = "是否为默认费用范围0-1")
        private Integer isDefault = 0;
    }
}
