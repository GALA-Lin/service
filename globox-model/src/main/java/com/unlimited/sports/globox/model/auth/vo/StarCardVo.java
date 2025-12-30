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
public class StarCardVo {

    @Schema(description = "用户名", example = "Ace Player")
    private String nickName;

    @Schema(description = "球星卡肖像URL（前端直传）", example = "https://cdn.example.com/portrait.png")
    private String portraitUrl;

    @Schema(description = "个性签名", example = "Play hard, stay sharp")
    private String signature;

    @Schema(description = "NTRP", example = "4.5")
    private BigDecimal ntrp;

    @Schema(description = "球风标签列表")
    private List<StyleTagVo> styleTags;

    @Schema(description = "球龄", example = "5")
    private Integer sportsYears;

    @Schema(description = "持拍手", example = "RIGHT")
    private String preferredHand;

    @Schema(description = "主力拍型号名称", example = "Pure Drive 98")
    private String mainRacketModelName;

    @Schema(description = "常驻区域", example = "Beijing Chaoyang")
    private String homeDistrict;

    @Schema(description = "力量", example = "80")
    private Integer power;

    @Schema(description = "速度", example = "75")
    private Integer speed;

    @Schema(description = "截击", example = "70")
    private Integer volley;

    @Schema(description = "耐力", example = "85")
    private Integer stamina;

    @Schema(description = "心理", example = "90")
    private Integer mental;
}

