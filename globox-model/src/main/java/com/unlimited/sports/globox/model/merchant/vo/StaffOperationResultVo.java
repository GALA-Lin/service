package com.unlimited.sports.globox.model.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工管理结果VO
 * @since 2026-01-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOperationResultVo {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 员工ID
     */
    private Long venueStaffId;

    /**
     * 员工名称
     */
    private String displayName;

    /**
     * 操作类型：CREATE=创建，UPDATE=更新，DELETE=删除
     */
    private String operationType;

    /**
     * 提示消息
     */
    private String message;

    /**
     * 快速构建成功结果
     */
    public static StaffOperationResultVo success(Long venueStaffId, String displayName,
                                                 String operationType, String message) {
        return StaffOperationResultVo.builder()
                .success(true)
                .venueStaffId(venueStaffId)
                .displayName(displayName)
                .operationType(operationType)
                .message(message)
                .build();
    }

    /**
     * 快速构建失败结果
     */
    public static StaffOperationResultVo failure(String operationType, String message) {
        return StaffOperationResultVo.builder()
                .success(false)
                .operationType(operationType)
                .message(message)
                .build();
    }
}