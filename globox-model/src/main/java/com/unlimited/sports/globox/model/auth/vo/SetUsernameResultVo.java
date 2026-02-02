package com.unlimited.sports.globox.model.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设置球盒号响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设置球盒号响应")
public class SetUsernameResultVo {

    @Schema(description = "球盒号（保留用户输入大小写）", example = "GloboxPlayer123")
    private String username;

    @Schema(description = "下次允许修改时间（冷却期结束时间，首次设置时返回）", 
            example = "2026-03-28T12:00:00")
    private LocalDateTime cooldownUntil;
}


