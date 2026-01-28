package com.unlimited.sports.globox.merchant.util;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.model.auth.enums.MerchantRole;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.entity.VenueStaff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.unlimited.sports.globox.common.result.UserAuthCode.TOKEN_EXPIRED;

/**
 * 商家权限校验工具类
 * 优化版：直接使用认证中心传来的 merchantId，减少数据库查询
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantAuthUtil {

    private final VenueStaffMapper venueStaffMapper;
    private final VenueMapper venueMapper;
    private final CourtMapper courtMapper;

    /**
     * 校验并获取商家认证上下文（优化版）
     *
     * @param userId 用户ID（由认证中心传入）
     * @param merchantId 商家ID（由认证中心传入，仅商家所有者有值）
     * @param roleStr 角色字符串
     * @return 商家认证上下文
     */
    public MerchantAuthContext validateAndGetContext(Long userId, Long merchantId, String roleStr) {
        // 参数校验
        if (userId == null) {
            log.error("请求头中缺少 X-User-Id");
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

        Long venueId = null;
        Long venueStaffId = null;

        if (role == MerchantRole.MERCHANT_OWNER) {
            // 商家所有者：直接使用认证中心传来的 merchantId
            if (merchantId == null) {
                log.error("商家所有者缺少 X-Merchant-Id - userId: {}", userId);
                throw new GloboxApplicationException("商家信息缺失");
            }

            log.info("商家所有者认证成功 - userId: {}, merchantId: {}", userId, merchantId);

        } else if (role == MerchantRole.MERCHANT_STAFF) {
            // 商家员工：需要查询员工信息获取 merchantId 和 venueId
            VenueStaff staff = venueStaffMapper.selectActiveStaffByUserId(userId);

            if (staff == null) {
                log.error("员工信息不存在或已离职 - userId: {}", userId);
                throw new GloboxApplicationException("员工信息不存在或已离职");
            }

            // 检查员工状态
            if (staff.getStatus() != MerchantConstants.STAFF_STATUS_ACTIVE) {
                log.error("员工状态异常 - venueStaffId: {}, status: {}",
                        staff.getVenueStaffId(), staff.getStatus());
                throw new GloboxApplicationException("您的员工状态异常，无法访问");
            }

            // 员工的 merchantId 从数据库查询
            merchantId = staff.getMerchantId();
            venueId = staff.getVenueId();
            venueStaffId = staff.getVenueStaffId();

            log.info("商家员工认证成功 - userId: {}, venueStaffId: {}, merchantId: {}, venueId: {}",
                    userId, venueStaffId, merchantId, venueId);
        } else {
            log.error("未知的角色类型: {}", role);
            throw new GloboxApplicationException("未知的角色类型");
        }

        return new MerchantAuthContext(userId, role, merchantId, venueId, venueStaffId);
    }

    /**
     * 校验场馆访问权限
     * 验证场馆是否属于该商家
     *
     * @param context 认证上下文
     * @param venueId 要访问的场馆ID
     */
    public void validateVenueAccess(MerchantAuthContext context, Long venueId) {
        if (venueId == null) {
            return;
        }

        // 查询场馆信息
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null) {
            log.error("场馆不存在 - venueId: {}", venueId);
            throw new GloboxApplicationException("场馆不存在");
        }

        // 验证场馆是否属于该商家
        if (!venue.getMerchantId().equals(context.getMerchantId())) {
            log.error("无权访问该场馆 - merchantId: {}, venueId: {}, venueMerchantId: {}",
                    context.getMerchantId(), venueId, venue.getMerchantId());
            throw new GloboxApplicationException("您无权访问该场馆");
        }

        // 如果是员工，还需要验证是否属于该场馆
        if (context.isStaff() && context.getVenueId() != null) {
            if (!venueId.equals(context.getVenueId())) {
                log.error("员工无权访问其他场馆 - venueStaffId: {}, requestVenueId: {}, staffVenueId: {}",
                        context.getVenueStaffId(), venueId, context.getVenueId());
                throw new GloboxApplicationException("您只能访问自己所在的场馆");
            }
        }

        log.debug("场馆访问权限验证通过 - merchantId: {}, venueId: {}",
                context.getMerchantId(), venueId);
    }

    /**
     * 校验场地访问权限
     * 验证场地是否属于该商家
     *
     * @param context 认证上下文
     * @param courtId 要访问的场地ID
     */
    public void validateCourtAccess(MerchantAuthContext context, Long courtId) {
        if (courtId == null) {
            return;
        }

        // 查询场地信息
        Court court = courtMapper.selectById(courtId);
        if (court == null) {
            log.error("场地不存在 - courtId: {}", courtId);
            throw new GloboxApplicationException("场地不存在");
        }

        // 通过场地的场馆ID验证权限
        validateVenueAccess(context, court.getVenueId());

        log.debug("场地访问权限验证通过 - merchantId: {}, courtId: {}",
                context.getMerchantId(), courtId);
    }

    /**
     * 批量验证场馆访问权限
     *
     * @param context  认证上下文
     * @param venueIds 场馆ID列表
     */
    public void validateVenueListAccess(MerchantAuthContext context, java.util.List<Long> venueIds) {
        if (venueIds == null || venueIds.isEmpty()) {
            return;
        }

        for (Long venueId : venueIds) {
            validateVenueAccess(context, venueId);
        }
    }

    /**
     * 批量验证场地访问权限
     *
     * @param context  认证上下文
     * @param courtIds 场地ID列表
     */
    public void validateCourtListAccess(MerchantAuthContext context, java.util.List<Long> courtIds) {
        if (courtIds == null || courtIds.isEmpty()) {
            return;
        }

        for (Long courtId : courtIds) {
            validateCourtAccess(context, courtId);
        }
    }

    /**
     * 要求必须是商家所有者
     *
     * @param context 认证上下文
     */
    public void requireOwner(MerchantAuthContext context) {
        if (!context.isOwner()) {
            log.error("权限不足，需要商家所有者权限 - userId: {}, role: {}",
                    context.getEmployeeId(), context.getRole());
            throw new GloboxApplicationException("权限不足，该操作需要商家所有者权限");
        }
        log.debug("商家所有者权限验证通过 - merchantId: {}", context.getMerchantId());
    }

    /**
     * 校验员工权限
     *
     * @param context    认证上下文
     * @param permission 所需权限
     */
    public void validatePermission(MerchantAuthContext context, String permission) {
        // 商家所有者拥有所有权限
        if (context.isOwner()) {
            log.debug("【{}】权限校验通过(商家所有者) - merchantId: {}",
                    permission, context.getMerchantId());
            return;
        }

        // TODO: 实现细粒度权限检查
        // 目前暂时允许所有在职员工访问
        log.debug("【{}】权限校验通过(员工) - venueStaffId: {}, merchantId: {}",
                permission, context.getVenueStaffId(), context.getMerchantId());
    }

    /**
     * 获取商家下所有场馆ID列表（用于权限范围过滤）
     *
     * @param context 认证上下文
     * @return 场馆ID列表
     */
    public java.util.List<Long> getAccessibleVenueIds(MerchantAuthContext context) {
        if (context.isOwner()) {
            // 商家所有者：返回旗下所有场馆
            return venueMapper.selectVenueIdsByMerchantId(context.getMerchantId());
        } else {
            // 员工：只返回所在场馆
            return context.getVenueId() != null
                    ? java.util.Collections.singletonList(context.getVenueId())
                    : java.util.Collections.emptyList();
        }
    }

    /**
     * 快速验证场馆归属（仅验证merchantId，不验证员工场馆限制）
     * 适用于需要快速验证但不需要严格员工场馆限制的场景
     *
     * @param context 认证上下文
     * @param venueId 场馆ID
     * @return 场馆信息
     */
    public Venue validateVenueBelongsToMerchant(MerchantAuthContext context, Long venueId) {
        if (venueId == null) {
            throw new GloboxApplicationException("场馆ID不能为空");
        }

        Venue venue = venueMapper.selectById(venueId);
        if (venue == null) {
            log.error("场馆不存在 - venueId: {}", venueId);
            throw new GloboxApplicationException("场馆不存在");
        }

        if (!venue.getMerchantId().equals(context.getMerchantId())) {
            log.error("场馆不属于该商家 - merchantId: {}, venueId: {}, venueMerchantId: {}",
                    context.getMerchantId(), venueId, venue.getMerchantId());
            throw new GloboxApplicationException("您无权访问该场馆");
        }

        return venue;
    }

    /**
     * 快速验证场地归属（仅验证merchantId）
     *
     * @param context 认证上下文
     * @param courtId 场地ID
     * @return 场地信息
     */
    public Court validateCourtBelongsToMerchant(MerchantAuthContext context, Long courtId) {
        if (courtId == null) {
            throw new GloboxApplicationException("场地ID不能为空");
        }

        Court court = courtMapper.selectById(courtId);
        if (court == null) {
            log.error("场地不存在 - courtId: {}", courtId);
            throw new GloboxApplicationException("场地不存在");
        }

        // 验证场地所属场馆是否属于该商家
        validateVenueBelongsToMerchant(context, court.getVenueId());

        return court;
    }
}