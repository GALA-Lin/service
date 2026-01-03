package com.unlimited.sports.globox.model.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 加入申请Dto
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RallyApplicationDto {

    /**
     * 帖子ID
     */
    @NotNull
    private Long postId;
    /**
     * 申请人ID
     */
    @NotNull
    private Long applicantId;
    /**
     * 申请时间
     */
    private LocalDateTime appliedAt;
    /**
     * 申请理由
     */
    private String reason;
}
