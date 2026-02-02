package com.unlimited.sports.globox.model.social.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 笔记标签枚举
 * 用于对笔记进行分类，支持多标签
 */
@Getter
public enum NoteTag {

    /**
     * 网球社区 - 社区讨论、经验分享等
     */
    TENNIS_COMMUNITY("TENNIS_COMMUNITY", "网球社区"),

    /**
     * 赛事活动 - 比赛、活动组织等
     */
    EVENT_ACTIVITY("EVENT_ACTIVITY", "赛事活动"),

    /**
     * 网坛快讯 - 网球新闻、热点资讯等
     */
    TENNIS_NEWS("TENNIS_NEWS", "网坛快讯"),

    /**
     * 装备测评 - 球拍、服装、鞋类等装备评测
     */
    EQUIPMENT_REVIEW("EQUIPMENT_REVIEW", "装备测评"),
    ;
    /**
     * 标签代码
     */
    private final String code;

    /**
     * 标签显示名称
     */
    private final String description;

    NoteTag(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举值
     */
    public static NoteTag fromCode(String code) {
        return Arrays.stream(NoteTag.values())
                .filter(tag -> tag.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有标签的代码列表
     */
    public static List<String> getAllCodes() {
        return Arrays.stream(NoteTag.values())
                .map(NoteTag::getCode)
                .collect(Collectors.toList());
    }

    /**
     * 获取标签的字典项列表
     */
    public static List<DictItem> getDictItems() {
        return Arrays.stream(NoteTag.values())
                .map(tag -> new DictItem(tag.code, tag.description))
                .collect(Collectors.toList());
    }

    /**
     * 批量转换标签代码为描述列表
     * @param tagCodes 标签代码列表
     * @return 标签描述列表
     */
    public static List<String> toDescriptions(List<String> tagCodes) {
        if (tagCodes == null || tagCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return tagCodes.stream()
                .map(code -> {
                    NoteTag tag = fromCode(code);
                    return tag != null ? tag.description : code;
                })
                .collect(Collectors.toList());
    }

    /**
     * 字典项内部类
     */
    public static class DictItem {
        private String code;
        private String description;

        public DictItem(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}
