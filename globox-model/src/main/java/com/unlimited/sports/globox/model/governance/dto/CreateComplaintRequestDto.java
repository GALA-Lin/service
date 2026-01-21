package com.unlimited.sports.globox.model.governance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateReportRequestDto", description = "提交举报请求")
public class CreateComplaintRequestDto {

    @NotNull
    @Schema(
            description = "举报对象类型：1=IM消息 2=帖子 3=帖子评论 4=场馆评论 5=用户信息",
            example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer targetType;

    @NotNull
    @Schema(
            description = "举报目标ID（对应业务表主键）",
            example = "900100200300",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long targetId;

    @Schema(description = "被举报内容作者/所属用户ID（冗余，可选）", example = "20001")
    private Long targetUserId;

    @NotNull
    @Schema(
            description = "举报原因 code（例如：谣言/恐慌/诈骗/色情等）",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer reason;

    @Size(max = 512)
    @Schema(description = "举报补充描述（可选，最多512字）", example = "该内容存在明显虚假信息，请核查。")
    private String description;

    /**
     * 证据截图URL（最多9张）
     */
    @Size(max = 9)
    @Schema(
            description = "证据截图URL列表（最多9张，可选）",
            example = "[\"https://xxx/1.png\",\"https://xxx/2.png\"]"
    )
    private List<@NotBlank String> evidenceUrls;
}