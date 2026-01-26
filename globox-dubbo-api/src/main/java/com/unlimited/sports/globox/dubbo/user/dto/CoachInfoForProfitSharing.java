package com.unlimited.sports.globox.dubbo.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 支付服务分账时，所需要的教练信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachInfoForProfitSharing implements Serializable {

    private Long coachId;

    /**
     * 教练的分账收款账户
     */
    private String account;

    /**
     * 教练真实姓名
     */
    private String realName;

}
