package com.unlimited.sports.globox.venue.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 槽位锁定请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotLockRequest {

    /**
     * 第三方场地ID
     */
    private String thirdPartyCourtId;

    /**
     * 开始时间（格式：HH:mm，例如"08:00"）
     */
    private String startTime;

    /**
     * 结束时间（格式：HH:mm，例如"09:00"）
     */
    private String endTime;

    /**
     * 预订日期
     */
    private LocalDate date;

    /**
     * 第三方预订ID（用于解锁时传递）
     * 锁定时由第三方平台返回，解锁时需要传入
     */
    private String thirdPartyBookingId;

    /**
     * 第三方锁场备注（格式：qiuhe_{时段}_{时间戳}）
     * 用于解锁时校验，确保只解锁我们系统锁定的场地
     */
    private String thirdPartyRemark;
}
