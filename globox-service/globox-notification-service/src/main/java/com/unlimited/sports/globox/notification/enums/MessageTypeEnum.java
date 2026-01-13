package com.unlimited.sports.globox.notification.enums;

import com.unlimited.sports.globox.common.enums.notification.NotificationModuleEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 消息类型枚举（用户端分类）
 * 将用户端消息类型映射到 NotificationModuleEnum 列表（支持一个消息类型对应多个模块）
 */
@Getter
@AllArgsConstructor
public enum MessageTypeEnum {

    /**
     * 探索消息：社交互动相关（点赞、评论、关注、@提及）
     */
    EXPLORE("explore", Arrays.asList(NotificationModuleEnum.SOCIAL), "探索消息"),

    /**
     * 球局消息：约球相关（申请、取消、人满）
     */
    RALLY("rally", Arrays.asList(NotificationModuleEnum.PLAY_MATCHING), "球局消息"),

    /**
     * 系统消息：除探索和球局外的所有消息（场地预约、教练预定、系统公告等）
     */
    SYSTEM("system", Arrays.asList(
            NotificationModuleEnum.VENUE_BOOKING,
            NotificationModuleEnum.COACH_BOOKING,
            NotificationModuleEnum.SYSTEM
    ), "系统消息");

    /**
     * 消息类型代码（用户端）
     */
    private final String code;

    /**
     * 对应的通知模块枚举列表（支持多个模块）
     */
    private final List<NotificationModuleEnum> modules;

    /**
     * 描述
     */
    private final String description;

    /**
     * 获取模块代码列表
     */
    public List<Integer> getModuleCodes() {
        return modules.stream()
                .map(NotificationModuleEnum::getCode)
                .toList();
    }

    /**
     * 根据模块枚举获取消息类型
     *
     * @param module 通知模块枚举
     * @return 消息类型，如果不匹配返回 null
     */
    public static MessageTypeEnum fromModule(NotificationModuleEnum module) {
        if (module == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.modules.contains(module))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据模块代码获取消息类型
     *
     * @param moduleCode notification_module 值
     * @return 消息类型，如果不匹配返回 null
     */
    public static MessageTypeEnum fromModuleCode(Integer moduleCode) {
        if (moduleCode == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.modules.stream()
                        .anyMatch(m -> m.getCode().equals(moduleCode)))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据代码获取消息类型
     *
     * @param code 消息类型代码
     * @return 消息类型，如果不匹配返回 null
     */
    public static MessageTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
