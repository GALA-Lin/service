package com.unlimited.sports.globox.model.venue.entity.venues;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 价格时段实体
 */
@Data
@TableName("venue_price_template_period")
public class VenuePriceTemplatePeriod {

    /**
     * 时段ID
     */
    @TableId(value = "period_id", type = IdType.AUTO)
    private Long periodId;

    /**
     * 所属价格模板ID
     */
    @TableField("template_id")
    private Long templateId;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalTime endTime;

    /**
     * 工作日价格（周一到周五）
     */
    @TableField("weekday_price")
    private BigDecimal weekdayPrice;

    /**
     * 周末价格（周六、周日）
     */
    @TableField("weekend_price")
    private BigDecimal weekendPrice;

    /**
     * 节假日价格
     */
    @TableField("holiday_price")
    private BigDecimal holidayPrice;

    /**
     * 是否启用
     */
    @TableField("is_enabled")
    private Boolean isEnabled;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
