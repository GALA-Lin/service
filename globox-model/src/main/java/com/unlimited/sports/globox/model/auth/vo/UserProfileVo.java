package com.unlimited.sports.globox.model.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户资料视图
 */
@Data
@Schema(description = "用户资料视图")
public class UserProfileVo {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "头像URL", example = "https://cdn.example.com/avatar.png")
    private String avatarUrl;

    @Schema(description = "球星卡肖像URL（前端直传）", example = "https://cdn.example.com/portrait.png")
    private String portraitUrl;

    @Schema(description = "昵称", example = "Ace Player")
    private String nickName;

    @Schema(description = "球盒号（区分大小写展示）", example = "GloBox123")
    private String username;

    @Schema(description = "当前用户是否关注TA", example = "false")
    private Boolean isFollowed;

    @Schema(description = "是否互相关注", example = "false")
    private Boolean isMutual;

    @Schema(description = "用户身份", example = "USER")
    private String role;

    @Schema(description = "个性签名", example = "Play hard, stay sharp")
    private String signature;

    @Schema(description = "性别", example = "MALE")
    private String gender;

    @Schema(description = "球龄年数（动态计算）", example = "2")
    private Integer sportsYears;

    @Schema(description = "NTRP", example = "4.5")
    private BigDecimal ntrp;

    @Schema(description = "持拍手", example = "RIGHT")
    private String preferredHand;

    @Schema(description = "常驻区域 code（region.code）", example = "510104")
    private String homeDistrict;

    @Schema(description = "常驻区域名称", example = "成都市武侯区")
    private String homeDistrictName;

    @Schema(description = "力量", example = "80")
    private Integer power;

    @Schema(description = "速度", example = "75")
    private Integer speed;

    @Schema(description = "发球", example = "7")
    private Integer serve;

    @Schema(description = "截击", example = "70")
    private Integer volley;

    @Schema(description = "耐力", example = "85")
    private Integer stamina;

    @Schema(description = "心理", example = "90")
    private Integer mental;

    @Schema(description = "球拍列表（包含是否主力拍）")
    private List<UserRacketVo> rackets;

    @Schema(description = "球风标签列表")
    private List<StyleTagVo> styleTags;
}
