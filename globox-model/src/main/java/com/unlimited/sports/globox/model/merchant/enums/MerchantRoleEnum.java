package com.unlimited.sports.globox.model.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025-12-18-14:04
 * 工作人员角色枚举
 */
@Getter
@AllArgsConstructor
public enum MerchantRoleEnum {

    VENUE_OWNER("VENUE_OWNER", "场馆负责人", "拥有所有权限"),
    MANAGER("MANAGER", "场馆管理员", "订单管理、时段管理"),
    STAFF("STAFF", "员工", "查看订单、确认到场");

    private final String code;
    private final String name;
    private final String description;


    public static MerchantRoleEnum getByCode(String code) {
        for (MerchantRoleEnum role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }
}