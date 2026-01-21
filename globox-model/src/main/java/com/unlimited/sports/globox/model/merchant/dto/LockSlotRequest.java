package com.unlimited.sports.globox.model.merchant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * @since 2025/12/28
 * 锁场请求DTO
 */
@Data
public class LockSlotRequest {

    /**
     * 模板ID列表（单个锁场时只传一个）
     */
    @NotEmpty(message = "模板ID列表不能为空")
    @Size(max = 100, message = "一次最多锁定100个时段")
    private List<Long> templateIds;

    /**
     * 预约日期
     */
    @NotNull(message = "预约日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    /**
     * 锁定原因
     */
    @NotBlank(message = "锁定原因不能为空")
    @Size(max = 500, message = "锁定原因长度不能超过500字符")
    private String reason;

    /**
     * 使用人姓名
     */
    @NotNull
    @Size(max = 50, message = "使用人姓名长度不能超过50字符")
    private String userName;

    /**
     * 使用人手机号
     */
    @NotNull
    @Size(max = 20, message = "使用人手机号长度不能超过20字符")
    private String userPhone;
}