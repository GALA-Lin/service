package com.unlimited.sports.globox.venue.adapter.dto.changxiaoer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 场小二解锁槽位请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangxiaoerUnlockSlotRequest {

    /**
     * 是否退款
     */
    private Boolean refundOrNot;

    /**
     * 详情ID列表
     */
    private List<String> detailIds;

    /**
     * 管理员ID
     */
    private String adminId;
}
