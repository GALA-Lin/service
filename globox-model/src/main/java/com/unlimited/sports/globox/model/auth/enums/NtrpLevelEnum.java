package com.unlimited.sports.globox.model.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

/**
 * @since 2026/1/1 12:56
 *
 */
@Getter
@AllArgsConstructor
public enum NtrpLevelEnum {
    LEVEL_1_0(1.0, "新手", "刚开始打网球"),
    LEVEL_1_5(1.5, "初学者", "对网球有限的打球经历"),
    LEVEL_2_0(2.0, "入门", "需要场上经验"),
    LEVEL_2_5(2.5, "初级", "正在学习击球判断及位置"),
    LEVEL_3_0(3.0, "业余初级", "击球比较稳定，缺乏场上经验"),
    LEVEL_3_5(3.5, "业余中级", "击球稳定可靠，基本战术理解"),
    LEVEL_4_0(4.0, "业余中高级", "击球可靠且有稳定的落点控制"),
    LEVEL_4_5(4.5, "业余高级", "掌握各项技术，开始掌握战术"),
    LEVEL_5_0(5.0, "优秀业余球员", "良好的击球技术和战术运用"),
    LEVEL_5_5(5.5, "区域级别选手", "有很强的实力可参加区域赛事"),
    LEVEL_6_0(6.0, "省级选手", "有丰富的比赛经验"),
    LEVEL_6_5(6.5, "国家级选手", "全国顶尖水平"),
    LEVEL_7_0(7.0, "世界级选手", "职业球员水平");

    private final Double level;
    private final String name;
    private final String description;

    /**
     * 根据水平值获取描述
     */
    public static String getDescription(Double level) {
        if (level == null) {
            return "未评级";
        }

        for (NtrpLevelEnum ntrp : values()) {
            if (ntrp.getLevel().equals(level)) {
                return ntrp.getName() + " - " + ntrp.getDescription();
            }
        }

        // 如果没有精确匹配，返回范围描述
        return getFuzzyDescription(level);
    }

    /**
     * 根据水平值获取简短名称
     */
    public static String getShortName(Double level) {
        if (level == null) {
            return "未评级";
        }

        for (NtrpLevelEnum ntrp : values()) {
            if (ntrp.getLevel().equals(level)) {
                return ntrp.getName();
            }
        }
        // 如果没有精确匹配，返回范围描述
        return getFuzzyDescription(level);
    }

    /**
     * 根据水平值获取范围描述
     * @param level 水平值
     * @return 范围描述
     */
    @NonNull
    private static String getFuzzyDescription(Double level) {
        if (level < LEVEL_2_0.getLevel()) {
            return "初学者";
        } else if (level < LEVEL_3_0.getLevel()) {
            return "入门";
        } else if (level < LEVEL_4_0.getLevel()) {
            return "业余初中级";
        } else if (level < LEVEL_5_0.getLevel()) {
            return "业余中高级";
        } else if (level < LEVEL_6_0.getLevel()) {
            return "优秀业余球员";
        } else {
            return "高水平选手";
        }
    }
}
