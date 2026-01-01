package com.unlimited.sports.globox.dubbo.order.dto;

import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MerchantRefundApplyPageRequestDto implements Serializable {

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    /**
     * 场馆ID列表
     */
    @NotNull(message = "场馆ID不能为空")
    @Size(message = "最少查询一个场地", min = 1)
    private List<Long> venueIds;

    /**
     * 退款申请状态过滤（可选）
     */
    private ApplyRefundStatusEnum applyStatus;

    /**
     * 订单号过滤（可选）
     */
    private Long orderNo;

    /**
     * 用户ID过滤（可选）
     */
    private Long userId;

    /**
     * 申请时间范围（可选）
     */
    private LocalDateTime appliedAtStart;
    private LocalDateTime appliedAtEnd;

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须>=1")
    private Integer pageNum = 1;

    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须>=1")
    private Integer pageSize = 10;
}