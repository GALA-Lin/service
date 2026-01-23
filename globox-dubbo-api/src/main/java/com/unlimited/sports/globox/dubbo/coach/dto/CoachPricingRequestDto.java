package com.unlimited.sports.globox.dubbo.coach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * @since 2026/1/6 14:22
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CoachPricingRequestDto implements Serializable {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 预约日期
     */
    @NotNull(message = "预约日期不能为空")
    private LocalDate bookingDate;


    /**
     * 预约的时段模板/记录ID列表（支持连续多个时段）
     */
    @NotNull(message = "时段模板/记录ID列表不能为空")
    private List<Long> slotIds;

    /**
     * 教练用户ID
     */
    @NotNull(message = "教练ID不能为空")
    private Long coachUserId;

    /**
     * 服务类型ID
     */
    @NotNull(message = "服务类型ID不能为空,查询价格接口需要传入服务类型ID")
    private Long serviceTypeId;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系电话
     */
//    @NotNull(message = "手机号不能为空")
    private String userPhone;

    /**
     * 学员人数
     */
    private Integer studentCount;

    /**
     * 特殊需求说明
     */
    private String specialRequirements;
}