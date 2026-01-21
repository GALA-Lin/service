package com.unlimited.sports.globox.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.governance.dto.CreateComplaintRequestDto;
import com.unlimited.sports.globox.service.FileUploadService;
import com.unlimited.sports.globox.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 举报工单 控制层
 */
@Tag(name = "举报", description = "举报工单相关接口")
@Validated
@RestController
@RequestMapping("governance/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    private final FileUploadService fileUploadService;

    @Operation(summary = "提交举报", description = "用户提交举报（IM会话/帖子/评论/场馆评论/用户信息），可携带描述与最多9张截图URL。")
    @PostMapping
    public R<Void> createReport(
            @Parameter(description = "创建举报工单请求参数", required = true)
            @RequestBody @Valid CreateComplaintRequestDto dto,
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) @NotNull Long userId,
            @RequestHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE) @NotNull String clientTypeStr) {

        ClientType clientType = ClientType.fromValue(clientTypeStr);
        complaintService.createComplaint(dto, userId, clientType);
        return R.ok();
    }


    @Operation(summary = "上传举报证据截图", description = "上传举报证据截图文件，返回可访问的文件URL（用于提交举报时 evidenceUrls）。")
    @PostMapping("upload")
    public R<String> uploadReviewImage(
            @Parameter(description = "图片文件（multipart/form-data，参数名必须为 file）",
                    required = true,
                    content = @Content(mediaType = "multipart/form-data",
                            schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file) {
        String fileUrl = fileUploadService.uploadFile(file, FileTypeEnum.COMPLAINT_EVIDENCE);
        return R.ok(fileUrl);
    }
}