package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.OperatorTypeEnum;
import com.unlimited.sports.globox.common.enums.order.OrderActionEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundTimelineVo implements Serializable {

    @NotNull
    private OrderActionEnum action;
    @NotNull
    private String actionName;
    @NotNull
    private LocalDateTime at;
    @Null
    private String remark;

    /**
     * 操作人信息
     */
    @NotNull
    private OperatorTypeEnum operatorType;
    @NotNull
    private Long operatorId;
    @NotNull
    private String operatorName;
}