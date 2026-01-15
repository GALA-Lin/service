package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;

/**
 * 用户资料更新请求
 */
@Data
@Schema(description = "用户资料更新请求")
public class UpdateUserProfileRequest {

    @Schema(description = "头像URL", example = "https://cdn.example.com/avatar.png")
    private String avatarUrl;

    @Schema(description = "球星卡肖像URL（前端直传）", example = "https://cdn.example.com/portrait.png")
    private String portraitUrl;

    @Schema(description = "昵称", example = "Ace Player")
    private String nickName;

    @Schema(description = "个性签名", example = "Play hard, stay sharp")
    private String signature;

    @Schema(description = "性别", example = "MALE")
    private String gender;

    @Schema(description = "球龄年数（字符串数字，后端换算起始年份）", example = "2")
    private String sportsYears;

    @Schema(description = "NTRP", example = "4.5")
    private BigDecimal ntrp;

    @Schema(description = "持拍手", example = "RIGHT")
    private String preferredHand;

    @Schema(description = "常驻区域 code（region.code）", example = "510104")
    private String homeDistrict;

    @Min(value = 0, message = "力量值必须在0-10之间")
    @Max(value = 10, message = "力量值必须在0-10之间")
    @Schema(description = "力量", example = "8")
    private Integer power;

    @Min(value = 0, message = "速度值必须在0-10之间")
    @Max(value = 10, message = "速度值必须在0-10之间")
    @Schema(description = "速度", example = "7")
    private Integer speed;

    @Min(value = 0, message = "发球值必须在0-10之间")
    @Max(value = 10, message = "发球值必须在0-10之间")
    @Schema(description = "发球", example = "7")
    private Integer serve;

    @Min(value = 0, message = "截击值必须在0-10之间")
    @Max(value = 10, message = "截击值必须在0-10之间")
    @Schema(description = "截击", example = "7")
    private Integer volley;

    @Min(value = 0, message = "耐力值必须在0-10之间")
    @Max(value = 10, message = "耐力值必须在0-10之间")
    @Schema(description = "耐力", example = "8")
    private Integer stamina;

    @Min(value = 0, message = "心理值必须在0-10之间")
    @Max(value = 10, message = "心理值必须在0-10之间")
    @Schema(description = "心理", example = "9")
    private Integer mental;

    @Valid
    @Schema(
            description = "球拍列表（全量替换，最多一个主力拍；传空列表清空）",
            example = "[{\"racketModelId\":1001,\"isPrimary\":true},{\"racketModelId\":1002,\"isPrimary\":false}]"
    )
    private List<UserRacketRequest> rackets;

    @Schema(description = "球风标签ID列表")
    private List<Long> tagIds;
}
