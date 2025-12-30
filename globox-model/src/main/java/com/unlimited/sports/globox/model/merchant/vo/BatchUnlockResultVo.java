package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @since 2025/12/29 17:43
 * 批量解锁结果
 */

@Data
@Builder
public class BatchUnlockResultVo {

    /**
     * 总数
     */
    private Integer total;

    /**
     * 成功数
     */
    private Integer success;

    /**
     * 失败数
     */
    private Integer failed;

    /**
     * 错误信息列表
     */
    private List<String> errorMessages;
}
