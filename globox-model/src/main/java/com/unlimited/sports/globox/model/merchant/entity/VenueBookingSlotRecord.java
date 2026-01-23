package com.unlimited.sports.globox.model.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @since 2025/12/27 14:51
 * 槽位预订记录实体
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("venue_booking_slot_record")
public class VenueBookingSlotRecord {

    /**
     * 预订记录ID
     */
    @TableId(value = "booking_slot_record_id", type = IdType.AUTO)
    private Long bookingSlotRecordId;

    /**
     * 槽位模板ID
     */
    @TableField("slot_template_id")
    private Long slotTemplateId;

    /**
     * 预订日期
     */
    @TableField("booking_date")
    private LocalDate bookingDate;

    /**
     * 槽位状态：1=可预订，2=占用中/锁定，3=不可预订
     */
    @TableField("status")
    private Integer status;

    /**
     * 锁定类型：1=商家锁场，2=用户订单
     */
    @TableField("locked_type")
    private Integer lockedType;

    @TableField("lock_reason")
    private String lockReason;

    /**
     * 批次标识（商家批量锁场时使用）
     */
    @TableField("merchant_batch_id")
    private Long merchantBatchId;

    /**
     * 关联订单号（占用时）
     */
    @TableField("order_no")
    private Long orderNo;
    /**
     * 操作人来源：1-商家端 2=用户端
     */
    @TableField("operator_source")
    private Integer operatorSource;

    /**
     * 操作人ID（用户ID或商家ID）
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 使用人名称
     * - 商家锁场：手动输入
     * - 用户下单：从用户表查询的昵称/手动输入
     */
    @TableField("user_name")
    private String userName;

    /**
     * 使用人手机号
     * 手动输入
     */
    @TableField("user_phone")
    private String userPhone;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
