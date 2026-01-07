package com.unlimited.sports.globox.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.common.utils.HttpRequestUtils;
import com.unlimited.sports.globox.common.utils.JwtUtil;
import com.unlimited.sports.globox.model.auth.dto.MerchantLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.MerchantLoginResponse;
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
            recordLoginLog(merchantAccount.getMerchantId(), LoginResult.FAIL);
            log.warn("【商家登录】失败：账号已禁用，merchantId={}", merchantAccount.getMerchantId());
            throw new GloboxApplicationException(UserAuthCode.MERCHANT_ACCOUNT_DISABLED);
        }

        //  验证密码
        boolean passwordMatch = PasswordUtils.matches(password, merchantAccount.getPasswordHash());
        if (!passwordMatch) {
            recordLoginLog(merchantAccount.getMerchantId(), LoginResult.FAIL);
            log.warn("【商家登录】失败：密码错误，merchantId={}", merchantAccount.getMerchantId());
            throw new GloboxApplicationException(UserAuthCode.MERCHANT_PASSWORD_ERROR);
        }

        // 生成JWT双Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", merchantAccount.getRole().name());

        String accessToken = JwtUtil.generateToken(
                String.valueOf(merchantAccount.getMerchantId()),
                claims,
                merchantJwtSecret,
                merchantAccessTokenExpire
        );

        String refreshToken = JwtUtil.generateToken(
                String.valueOf(merchantAccount.getMerchantId()),
                claims,
                merchantJwtSecret,
                merchantRefreshTokenExpire
        );

        // 保存Refresh Token到Redis
        redisService.saveRefreshToken(merchantAccount.getMerchantId(), refreshToken, merchantRefreshTokenExpire);

        // 记录登录成功日志
        recordLoginLog(merchantAccount.getMerchantId(), LoginResult.SUCCESS);

        MerchantLoginResponse response = MerchantLoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .merchantId(merchantAccount.getMerchantId())
                .account(merchantAccount.getAccount())
                .roles(Collections.singletonList(merchantAccount.getRole().name()))
                .build();

        log.info("【商家登录】成功：merchantId={}, account={}", merchantAccount.getMerchantId(), account);
        return response;
    }

    /**
     * 记录登录日志
     */
    private void recordLoginLog(Long merchantId, LoginResult result) {
        try {
            MerchantLoginRecord record = new MerchantLoginRecord();
            record.setMerchantId(merchantId);
            record.setResult(result);
            record.setIp(HttpRequestUtils.getRealIp());
            merchantLoginRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("【商家登录】记录登录日志失败", e);
        }
    }
}
