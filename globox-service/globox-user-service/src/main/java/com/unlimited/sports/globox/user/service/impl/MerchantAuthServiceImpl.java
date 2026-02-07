package com.unlimited.sports.globox.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.HttpRequestUtils;
import com.unlimited.sports.globox.common.utils.JwtUtil;
import com.unlimited.sports.globox.common.utils.RequestContextHolder;
import com.unlimited.sports.globox.model.auth.dto.ChangePasswordRequest;
import com.unlimited.sports.globox.model.auth.dto.MerchantLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.MerchantLoginResponse;
import com.unlimited.sports.globox.model.auth.dto.TokenRefreshRequest;
import com.unlimited.sports.globox.model.auth.entity.MerchantAccount;
import com.unlimited.sports.globox.model.auth.entity.MerchantLoginRecord;
import com.unlimited.sports.globox.model.auth.enums.LoginResult;
import com.unlimited.sports.globox.model.auth.enums.MerchantStatus;
import com.unlimited.sports.globox.user.mapper.MerchantAccountMapper;
import com.unlimited.sports.globox.user.mapper.MerchantLoginRecordMapper;
import com.unlimited.sports.globox.user.service.MerchantAuthService;
import com.unlimited.sports.globox.user.service.RedisService;
import com.unlimited.sports.globox.user.util.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 商家认证服务实现
 */
@Service
@Slf4j
public class MerchantAuthServiceImpl implements MerchantAuthService {

    @Autowired
    private MerchantAccountMapper merchantAccountMapper;

    @Autowired
    private MerchantLoginRecordMapper merchantLoginRecordMapper;

    @Autowired
    private RedisService redisService;

    @Value("${merchant.jwt.secret}")
    private String merchantJwtSecret;

    @Value("${merchant.jwt.access-token-expire:1800}")
    private long merchantAccessTokenExpire;

    @Value("${merchant.jwt.refresh-token-expire:604800}")
    private long merchantRefreshTokenExpire;

    @Override
    public MerchantLoginResponse merchantLogin(MerchantLoginRequest request) {
        String account = request.getAccount();
        String password = request.getPassword();


        // 查询账号是否存在
        LambdaQueryWrapper<MerchantAccount> query = new LambdaQueryWrapper<>();
        query.eq(MerchantAccount::getAccount, account);
        MerchantAccount merchantAccount = merchantAccountMapper.selectOne(query);

        if (merchantAccount == null) {
            // 账号不存在
            recordLoginLog(null, LoginResult.FAIL);
            log.warn("【商家登录】失败：账号不存在，account={}", account);
            throw new GloboxApplicationException(UserAuthCode.MERCHANT_ACCOUNT_NOT_EXIST);
        }

        // 检查账号状态
        if (merchantAccount.getStatus() == MerchantStatus.DISABLED) {
            recordLoginLog(merchantAccount.getAccountId(), LoginResult.FAIL);
            log.warn("【商家登录】失败：账号已禁用，accountId={}", merchantAccount.getAccountId());
            throw new GloboxApplicationException(UserAuthCode.MERCHANT_ACCOUNT_DISABLED);
        }

        //  验证密码
        boolean passwordMatch = PasswordUtils.matches(password, merchantAccount.getPasswordHash());
        if (!passwordMatch) {
            recordLoginLog(merchantAccount.getAccountId(), LoginResult.FAIL);
            log.warn("【商家登录】失败：密码错误，accountId={}", merchantAccount.getAccountId());
            throw new GloboxApplicationException(UserAuthCode.MERCHANT_PASSWORD_ERROR);
        }

        // 生成JWT双Token
        Long employeeId = parseEmployeeId(merchantAccount.getEmployeeId(), merchantAccount.getAccountId());
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", merchantAccount.getRole().name());
        claims.put("employee_id", employeeId);
        claims.put("merchant_id", merchantAccount.getMerchantId());
        String accessToken = JwtUtil.generateToken(
                String.valueOf(merchantAccount.getAccountId()),
                claims,
                merchantJwtSecret,
                merchantAccessTokenExpire
        );

        String refreshToken = JwtUtil.generateToken(
                String.valueOf(merchantAccount.getAccountId()),
                claims,
                merchantJwtSecret,
                merchantRefreshTokenExpire
        );

        // 保存Refresh Token到Redis
        redisService.saveRefreshToken(merchantAccount.getAccountId(), refreshToken, merchantRefreshTokenExpire);

        // 记录登录成功日志
        recordLoginLog(merchantAccount.getAccountId(), LoginResult.SUCCESS);

        MerchantLoginResponse response = MerchantLoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .accountId(merchantAccount.getAccountId())
                .account(merchantAccount.getAccount())
                .roles(Collections.singletonList(merchantAccount.getRole().name()))
                .build();

        log.info("【商家登录】成功：accountId={}, account={}", merchantAccount.getAccountId(), account);
        return response;
    }

    @Override
    public MerchantLoginResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. 校验 JWT 签名和过期时间
        if (!JwtUtil.validateToken(refreshToken, merchantJwtSecret)) {
            throw new GloboxApplicationException(UserAuthCode.REFRESH_TOKEN_INVALID);
        }

        // 2. 校验 Redis 中是否有效（是否已被吊销）
        if (!redisService.isRefreshTokenValid(refreshToken)) {
            throw new GloboxApplicationException(UserAuthCode.REFRESH_TOKEN_INVALID);
        }

        // 3. 解析 accountId
        String accountIdStr = JwtUtil.getSubject(refreshToken, merchantJwtSecret);
        Long accountId = Long.parseLong(accountIdStr);

        // 4. 查询商家账号获取最新角色信息和账号状态
        LambdaQueryWrapper<MerchantAccount> query = new LambdaQueryWrapper<>();
        query.eq(MerchantAccount::getAccountId, accountId);
        MerchantAccount merchantAccount = merchantAccountMapper.selectOne(query);
        Assert.isNotEmpty(merchantAccount, UserAuthCode.MERCHANT_ACCOUNT_NOT_EXIST);

        // 5. 检查账号状态（DISABLED 则拒绝刷新）
        if (merchantAccount.getStatus() == MerchantStatus.DISABLED) {
            log.warn("【商家Token刷新】失败：账号已禁用，accountId={}", accountId);
            throw new GloboxApplicationException(UserAuthCode.MERCHANT_ACCOUNT_DISABLED);
        }

        // 6. 删除旧 refreshToken（token 旋转：旧 token 立即失效）
        redisService.deleteRefreshToken(refreshToken, merchantJwtSecret);

        // 7. 生成新的 access token 和 refresh token
        Long employeeId = parseEmployeeId(merchantAccount.getEmployeeId(), accountId);
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", merchantAccount.getRole().name());
        claims.put("employee_id", employeeId);
        claims.put("merchant_id", merchantAccount.getMerchantId());

        String newAccessToken = JwtUtil.generateToken(
                String.valueOf(accountId),
                claims,
                merchantJwtSecret,
                merchantAccessTokenExpire
        );

        String newRefreshToken = JwtUtil.generateToken(
                String.valueOf(accountId),
                claims,
                merchantJwtSecret,
                merchantRefreshTokenExpire
        );

        // 8. 保存新 refresh token 到 Redis
        redisService.saveRefreshToken(accountId, newRefreshToken, merchantRefreshTokenExpire);

        // 9. 构建响应
        MerchantLoginResponse response = MerchantLoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .accountId(accountId)
                .account(merchantAccount.getAccount())
                .roles(Collections.singletonList(merchantAccount.getRole().name()))
                .build();

        log.info("【商家Token刷新】成功：accountId={}, account={}", accountId, merchantAccount.getAccount());
        return response;
    }

    /**
     * 商家修改密码
     */
    @Override
    public String changePassword(ChangePasswordRequest request) {
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        String authHeader = RequestContextHolder.getHeader("Authorization");
        String token = JwtUtil.extractTokenFromHeader(authHeader);
        Assert.isNotEmpty(token, UserAuthCode.TOKEN_INVALID);
        if (!JwtUtil.validateToken(token, merchantJwtSecret)) {
            throw new GloboxApplicationException(UserAuthCode.TOKEN_INVALID);
        }

        Long accountId = Long.parseLong(JwtUtil.getSubject(token, merchantJwtSecret));
        MerchantAccount merchantAccount = merchantAccountMapper.selectById(accountId);
        Assert.isNotEmpty(merchantAccount, UserAuthCode.MERCHANT_ACCOUNT_NOT_EXIST);

        if (merchantAccount.getStatus() == MerchantStatus.DISABLED) {
            throw new GloboxApplicationException(UserAuthCode.MERCHANT_ACCOUNT_DISABLED);
        }

        boolean matches = PasswordUtils.matches(oldPassword, merchantAccount.getPasswordHash());
        if (!matches) {
            throw new GloboxApplicationException(UserAuthCode.MERCHANT_PASSWORD_ERROR);
        }

        Assert.isTrue(PasswordUtils.isValidPassword(newPassword), UserAuthCode.PASSWORD_TOO_WEAK);
        Assert.isTrue(newPassword.equals(confirmPassword), UserAuthCode.PASSWORD_MISMATCH);

        merchantAccount.setPasswordHash(PasswordUtils.encode(newPassword));
        merchantAccountMapper.updateById(merchantAccount);

        redisService.deleteAllRefreshTokens(accountId);

        log.info("商家密码修改成功：accountId={}", accountId);
        return "密码修改成功，请重新登录";
    }

    /**
     * 记录登录日志
     */
    private void recordLoginLog(Long accountId, LoginResult result) {
        try {
            MerchantLoginRecord record = new MerchantLoginRecord();
            record.setAccountId(accountId);
            record.setResult(result);
            record.setIp(HttpRequestUtils.getRealIp());
            merchantLoginRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("【商家登录】记录登录日志失败", e);
        }
    }

    private Long parseEmployeeId(String employeeId, Long accountId) {
        if (!StringUtils.hasText(employeeId)) {
            log.error("【商家登录】employeeId为空，accountId={}", accountId);
            throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
        }
        try {
            return Long.valueOf(employeeId);
        } catch (NumberFormatException e) {
            log.error("【商家登录】employeeId格式非法，employeeId={}, accountId={}", employeeId, accountId);
            throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
        }
    }
}
