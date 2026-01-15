package com.unlimited.sports.globox.model.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 球星卡视图
 */
@Data
@Schema(description = "球星卡视图")
public class PlanetCardVo {

    @Schema(description = "用户名", example = "Ace Player")
    private String nickName;

    @Schema(description = "个性签名", example = "Play hard, stay sharp")
    private String signature;

    @Schema(description = "NTRP", example = "4.5")
    private BigDecimal ntrp;

    @Schema(description = "球风标签列表")
    private List<StyleTagVo> styleTags;

    @Schema(description = "球龄年数（动态计算）", example = "2")
    private Integer sportsYears;

    @Schema(description = "持拍手", example = "RIGHT")
    private String preferredHand;

    @Schema(description = "主力拍型号名称", example = "Pure Drive 98")
    private String mainRacketModelName;

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
}
