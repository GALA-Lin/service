package com.unlimited.sports.globox.model.venue.entity.booking;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 场馆价格覆盖实体类
 * 用于管理场馆或场地在特定时间段的价格覆盖（如特殊赛事、维护、促销等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("venue_price_override")
public class VenuePriceOverride implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 价格覆盖ID，主键
     */
    @TableId(value = "override_id", type = IdType.AUTO)
    private Long overrideId;

    /**
     * 场馆ID
     */
    @TableField("venue_id")
    private Long venueId;

    /**
     * 场地ID（NULL表示对整个场馆生效，否则只对该场地生效）
     */
    @TableField("court_id")
    private Long courtId;

    /**
     * 改价日期
     */
    @TableField("override_date")
    private LocalDate overrideDate;

    /**
     * 改价开始时间
     */
    @TableField("start_time")
    private LocalTime startTime;

    /**
     * 改价结束时间
     */
    @TableField("end_time")
    private LocalTime endTime;

    /**
     * 覆盖价格（新价格）
     */
    @TableField("override_price")
    private BigDecimal overridePrice;

    /**
     * 改价原因描述（如：特殊赛事、维护、促销等）
     */
    @TableField("description")
    private String description;

    /**
     * 是否启用：0=否，1=是
     */
    @TableField("is_enabled")
    private Integer isEnabled;

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

    /**
     * 检查该价格覆盖是否对指定场地生效
     *
     * @param courtId 场地ID
     * @return true=生效，false=不生效
     */
    public boolean isApplicableToCourtId(Long courtId) {
        // 如果 court_id 为 null，表示对整个场馆生效
        if (this.courtId == null) {
            return true;
        }
        // 否则，只对指定场地生效
        return this.courtId.equals(courtId);
    }

    /**
     * 检查该价格覆盖是否在指定时间段内有效
     *
     * @param date      日期
     * @param timeStart 开始时间
     * @param timeEnd   结束时间
     * @return true=有效，false=无效
     */
    public boolean isValidForTimeRange(LocalDate date, LocalTime timeStart, LocalTime timeEnd) {
        // 日期必须匹配
        if (!this.overrideDate.equals(date)) {
            return false;
        }
        // 时间段必须有重叠
        // 重叠条件：this.startTime < timeEnd && this.endTime > timeStart
        return this.startTime.isBefore(timeEnd) && this.endTime.isAfter(timeStart);
    }
}
