package com.unlimited.sports.globox.merchant.util;

/**
 * 商家模块常量
 */
public class MerchantConstants {

    /**
     * 请求头：员工ID（商家所有者则是商家ID，员工则是venue_staff_id）
     */
    public static final String HEADER_EMPLOYEE_ID = "X-Employee-Id";

    /**
     * 请求头：商家角色（MERCHANT_OWNER 或 MERCHANT_STAFF）
     */
    public static final String HEADER_MERCHANT_ROLE = "X-Merchant-Role";

    /**
     * 员工状态：已离职
     */
    public static final int STAFF_STATUS_RESIGNED = 0;

    /**
     * 员工状态：在职
     */
    public static final int STAFF_STATUS_ACTIVE = 1;

    /**
     * 员工状态：停用
     */
    public static final int STAFF_STATUS_DISABLED = 2;

    /**
     * 员工权限：订单管理
     */
    public static final String PERMISSION_ORDER_MANAGE = "order_manage";

    /**
     * 员工权限：时段管理
     */
    public static final String PERMISSION_SLOT_MANAGE = "slot_manage";

    /**
     * 员工权限：统计查看
     */
    public static final String PERMISSION_STAT_VIEW = "stat_view";

    /**
     * 员工权限：价格管理
     */
    public static final String PERMISSION_PRICE_MANAGE = "price_manage";
}