package com.unlimited.sports.globox.model.venue.entity.venues;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.common.config.ListLongTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 场馆额外费用模板 - 商家预先配置的可选费用
 * 支持多种计费方式和适用范围的灵活配置
 */
@Data
@TableName("venue_extra_charge_template")
public class VenueExtraChargeTemplate {

    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 费用类型：1=LIGHT(灯光费)，2=COACH(教练费)，3=EQUIPMENT(器材费)，
     *          4=PARKING(停车费)，5=CLEANING(清洁费)，6=OTHER(其他)
     */
    private Integer chargeType;

    /**
     * 费用名称（商家自定义）：如"LED灯光费"、"专业教练"
     */
    private String chargeName;

    /**
     * 费用级别：1=ORDER_LEVEL(订单级别)，2=ORDER_ITEM_LEVEL(订单项级别)
     */
    private Integer chargeLevel;

    /**
     * 计费方式：1=FIXED(固定金额)，2=PERCENTAGE(百分比)
     * 对应ChargeModeEnum枚举
     */
    private Integer chargeMode;

    /**
     * 单位金额或比例
     * - FIXED(1)：固定费用金额
     * - PERCENTAGE(2)：百分比值（如5表示5%）
     */
    private BigDecimal unitAmount;

    /**
     * 适用场地ID列表，JSON格式。留空表示对所有场地适用
     * 如：[1,2,3]
     */
    @TableField(value = "applicable_court_ids", typeHandler = ListLongTypeHandler.class)
    private List<Long> applicableCourtIds;

    /**
     * 适用天数：0=ALL(所有天)，1=WEEKDAY(工作日)，2=WEEKEND(周末)
     */
    private Integer applicableDays;

    /**
     * 费用描述
     */
    private String description;

    /**
     * 是否启用：0=否，1=是
     */
    private Integer isEnabled;

    /**
     * 是否为默认费用（用户预订时自动选中）：0=否，1=是
     */
    private Integer isDefault;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
