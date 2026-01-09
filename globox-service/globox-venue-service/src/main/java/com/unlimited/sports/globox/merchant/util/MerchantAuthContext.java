package com.unlimited.sports.globox.merchant.util;

import com.unlimited.sports.globox.model.auth.enums.MerchantRole;
import lombok.Getter;

/**
 * @since 2026/1/9 10:35
 *
 */

@Getter
public class MerchantAuthContext {
    private final Long employeeId;
    private final MerchantRole role;
    private final Long merchantId;
    private final Long venueId;

    public MerchantAuthContext(Long employeeId, MerchantRole role, Long merchantId, Long venueId) {
        this.employeeId = employeeId;
        this.role = role;
        this.merchantId = merchantId;
        this.venueId = venueId;
    }

    public boolean isOwner() {
        return MerchantRole.MERCHANT_OWNER.equals(role);
    }

    public boolean isStaff() {
        return MerchantRole.MERCHANT_STAFF.equals(role);
    }
}