package com.unlimited.sports.globox.merchant.util;

import com.unlimited.sports.globox.model.auth.enums.MerchantRole;
import lombok.Getter;

/**
 * 商家认证上下文
 * 存储当前请求的商家/员工身份信息
 *
 * @since 2026/1/9 10:35
 */
@Getter
public class MerchantAuthContext {
    /**
     * 用户ID（employeeId就是userId）
     */
    private final Long employeeId;

    /**
     * 商家角色：MERCHANT_OWNER 或 MERCHANT_STAFF
     */
    private final MerchantRole role;

    /**
     * 商家ID
     */
    private final Long merchantId;

    /**
     * 场馆ID（仅员工角色有值）
     */
    private final Long venueId;

    /**
     * 员工关联ID（仅员工角色有值）
     */
    private final Long venueStaffId;

    public MerchantAuthContext(Long employeeId, MerchantRole role, Long merchantId,
                               Long venueId, Long venueStaffId) {
        this.employeeId = employeeId;
        this.role = role;
        this.merchantId = merchantId;
        this.venueId = venueId;
        this.venueStaffId = venueStaffId;
    }

    /**
     * 判断是否为商家所有者
     */
    public boolean isOwner() {
        return MerchantRole.MERCHANT_OWNER.equals(role);
    }

    /**
     * 判断是否为员工
     */
    public boolean isStaff() {
        return MerchantRole.MERCHANT_STAFF.equals(role);
    }

    /**
     * 获取可访问的场馆ID
     * - 商家所有者：null（表示可以访问所有旗下场馆）
     * - 员工：返回所在场馆ID
     */
    public Long getAccessibleVenueId() {
        return isStaff() ? venueId : null;
    }

    @Override
    public String toString() {
        return "MerchantAuthContext{" +
                "employeeId=" + employeeId +
                ", role=" + role +
                ", merchantId=" + merchantId +
                ", venueId=" + venueId +
                ", venueStaffId=" + venueStaffId +
                '}';
    }
}