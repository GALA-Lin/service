package com.unlimited.sports.globox.merchant.util;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.MerchantMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.model.auth.enums.MerchantRole;
import com.unlimited.sports.globox.model.merchant.entity.Merchant;
import com.unlimited.sports.globox.model.merchant.entity.VenueStaff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.unlimited.sports.globox.common.result.UserAuthCode.TOKEN_EXPIRED;

/**
 * 商家权限校验工具类
 * 根据请求头中的 X-Employee-Id 和 X-Merchant-Role 进行权限校验
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantAuthUtil {

    private final VenueStaffMapper venueStaffMapper;
    private final MerchantMapper merchantMapper;

    /**
     * 校验并获取商家认证上下文
     *
     * @param employeeId 职工ID（商家则是商家ID，职工则是venue_staff_id）
     * @param roleStr    角色字符串
     * @return 商家认证上下文
     */
    public MerchantAuthContext validateAndGetContext(Long employeeId, String roleStr) {
        // 参数校验
        if (employeeId == null) {
            log.error("请求头中缺少 X-Employee-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), "未登录或登录已过期");
        }

        if (roleStr == null || roleStr.trim().isEmpty()) {
            log.error("请求头中缺少 X-Merchant-Role");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), "角色信息缺失");
        }

        // 解析角色
        MerchantRole role;
        try {
            role = MerchantRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            log.error("无效的角色类型: {}", roleStr);
            throw new GloboxApplicationException("无效的角色类型");
        }

        // 根据角色获取商家ID
        Long merchantId;
        Long venueId = null;

        if (role == MerchantRole.MERCHANT_OWNER) {
            // 商家所有者：employeeId 就是 merchantId
            merchantId = employeeId;

            // 验证商家是否存在
            Merchant merchant = merchantMapper.selectById(merchantId);
            if (merchant == null) {
                log.error("商家不存在: merchantId={}", merchantId);
                throw new GloboxApplicationException("商家信息不存在");
            }

            log.debug("商家所有者认证成功 - merchantId: {}", merchantId);

        } else if (role == MerchantRole.MERCHANT_STAFF) {
            // 商家员工：employeeId 是 venue_staff_id
            VenueStaff staff = venueStaffMapper.selectById(employeeId);

            if (staff == null) {
                log.error("员工信息不存在: venueStaffId={}", employeeId);
                throw new GloboxApplicationException("员工信息不存在");
            }

            // 检查员工状态
            if (staff.getStatus() != 1) {
                log.error("员工状态异常: venueStaffId={}, status={}", employeeId, staff.getStatus());
                throw new GloboxApplicationException("您的员工状态异常，无法访问");
            }

            merchantId = staff.getMerchantId();
            venueId = staff.getVenueId();

            log.debug("商家员工认证成功 - venueStaffId: {}, merchantId: {}, venueId: {}",
                    employeeId, merchantId, venueId);
        } else {
            log.error("未知的角色类型: {}", role);
            throw new GloboxApplicationException("未知的角色类型");
        }

        return new MerchantAuthContext(employeeId, role, merchantId, venueId);
    }

    /**
     * 校验场馆访问权限（简化版本）
     * 只验证场馆是否属于该商家
     *
     * @param context  认证上下文
     * @param venueId  要访问的场馆ID
     */
    public void validateVenueAccess(MerchantAuthContext context, Long venueId) {
        if (venueId == null) {
            return;
        }

        // 商家所有者可以访问旗下所有场馆
        if (context.isOwner()) {
            // 暂不验证场馆归属，假定前端不会传错误的venueId
            log.debug("商家所有者访问场馆 - merchantId: {}, venueId: {}",
                    context.getMerchantId(), venueId);
            return;
        }

        // 员工只能访问自己所在的场馆
        if (context.isStaff()) {
            if (!venueId.equals(context.getVenueId())) {
                log.error("员工无权访问其他场馆 - venueStaffId: {}, requestVenueId: {}, staffVenueId: {}",
                        context.getEmployeeId(), venueId, context.getVenueId());
                throw new GloboxApplicationException("您无权访问该场馆");
            }
        }
    }

    /**
     * 校验场地访问权限（简化版本）
     * 暂时不做验证，假定前端不会传错误的courtId
     *
     * @param context  认证上下文
     * @param courtId  要访问的场地ID
     */
    public void validateCourtAccess(MerchantAuthContext context, Long courtId) {
        if (courtId == null) {
            return;
        }

        // 暂不验证场地归属
        log.debug("访问场地 - employeeId: {}, role: {}, courtId: {}",
                context.getEmployeeId(), context.getRole(), courtId);
    }

    /**
     * 要求必须是商家所有者（暂时不限制，员工也可以操作）
     *
     * @param context 认证上下文
     */
    public void requireOwner(MerchantAuthContext context) {
        // 暂不限制，只要是该商家的员工或所有者都可以
        log.debug("所有者权限检查 - employeeId: {}, role: {}",
                context.getEmployeeId(), context.getRole());
    }

    /**
     * 校验员工权限（暂时不做细致鉴权，只要是该商家的员工或所有者即可）
     *
     * @param context     认证上下文
     * @param permission  所需权限（预留参数，暂不使用）
     */
    public void validatePermission(MerchantAuthContext context, String permission) {
        // 只要通过了 validateAndGetContext 验证，说明是该商家的员工或所有者
        // 暂不做细致的权限检查
        log.debug("权限校验通过 - employeeId: {}, role: {}, merchantId: {}",
                context.getEmployeeId(), context.getRole(), context.getMerchantId());
    }
}