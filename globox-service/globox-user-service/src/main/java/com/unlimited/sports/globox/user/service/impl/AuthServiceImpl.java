package com.unlimited.sports.globox.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.RequestContextHolder;
import com.unlimited.sports.globox.common.utils.HttpRequestUtils;
import com.unlimited.sports.globox.common.utils.JwtUtil;
import com.unlimited.sports.globox.common.constants.RedisKeyConstants;
import com.unlimited.sports.globox.dubbo.order.OrderForUserDubboService;
import com.unlimited.sports.globox.dubbo.social.ChatDubboService;
import com.unlimited.sports.globox.model.auth.dto.AppleLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.CancelAccountConfirmRequest;
import com.unlimited.sports.globox.model.auth.dto.CancelAccountRequest;
import com.unlimited.sports.globox.model.auth.dto.ChangePasswordRequest;
import com.unlimited.sports.globox.model.auth.dto.DeviceInfo;
import com.unlimited.sports.globox.model.auth.dto.DeviceRegisterRequest;
import com.unlimited.sports.globox.model.auth.dto.LoginResponse;
import com.unlimited.sports.globox.model.auth.dto.PasswordLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.PhoneLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.ResetPasswordRequest;
import com.unlimited.sports.globox.model.auth.dto.SendCaptchaRequest;
import com.unlimited.sports.globox.model.auth.dto.SetPasswordRequest;
import com.unlimited.sports.globox.model.auth.dto.ThirdPartyLoginResponse;
import com.unlimited.sports.globox.model.auth.dto.TokenRefreshRequest;
import com.unlimited.sports.globox.model.auth.dto.WechatBindPhoneRequest;
import com.unlimited.sports.globox.model.auth.dto.WechatLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.WechatLoginResponse;
import com.unlimited.sports.globox.model.auth.dto.WechatPhoneLoginRequest;
import com.unlimited.sports.globox.model.auth.entity.AuthIdentity;
import com.unlimited.sports.globox.model.auth.entity.AuthUser;
import com.unlimited.sports.globox.model.auth.entity.InternalTestWhitelist;
import com.unlimited.sports.globox.model.auth.entity.UserLoginRecord;
import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import com.unlimited.sports.globox.model.auth.entity.UserRacket;
import com.unlimited.sports.globox.model.auth.entity.UserStyleTag;
import com.unlimited.sports.globox.model.auth.enums.GenderEnum;
import com.unlimited.sports.globox.model.auth.vo.WechatUserInfo;
import com.unlimited.sports.globox.user.mapper.AuthIdentityMapper;
import com.unlimited.sports.globox.user.mapper.AuthUserMapper;
import com.unlimited.sports.globox.user.mapper.UserLoginRecordMapper;
import com.unlimited.sports.globox.user.mapper.UserProfileMapper;
import com.unlimited.sports.globox.user.mapper.UserRacketMapper;
import com.unlimited.sports.globox.user.mapper.UserStyleTagMapper;
import com.unlimited.sports.globox.user.prop.UserProfileDefaultProperties;
import com.unlimited.sports.globox.user.service.AppleService;
import com.unlimited.sports.globox.user.service.AuthService;
import com.unlimited.sports.globox.user.service.IUserDeviceService;
import com.unlimited.sports.globox.user.service.RedisService;
import com.unlimited.sports.globox.user.service.SmsService;
import com.unlimited.sports.globox.user.service.WechatService;
import com.unlimited.sports.globox.user.service.WhitelistService;
import com.unlimited.sports.globox.user.util.PasswordUtils;
import com.unlimited.sports.globox.user.util.PhoneUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
/**
 * 认证服务实现
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private RedisService redisService;

    @Autowired
    @Lazy
    private AuthService thisService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private WhitelistService whitelistService;

    @Autowired
    private AuthUserMapper authUserMapper;

    @Autowired
    private AuthIdentityMapper authIdentityMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private UserLoginRecordMapper userLoginRecordMapper;

    @Autowired
    private UserRacketMapper userRacketMapper;

    @Autowired
    private UserStyleTagMapper userStyleTagMapper;

    @Autowired
    private WechatService wechatService;

    @Autowired
    private AppleService appleService;

    @Autowired
    private IUserDeviceService userDeviceService;

    @DubboReference(group = "rpc")
    private ChatDubboService chatDubboService;

    @DubboReference(group = "rpc")
    private OrderForUserDubboService orderForUserDubboService;

    @Autowired
    private UserProfileDefaultProperties userProfileDefaultProperties;

    @Autowired
    private Environment environment;

    @Value("${user.jwt.secret}")
    private String jwtSecret;

    @Value("${user.jwt.access-token-expire}")
    private long accessTokenExpire;

    @Value("${user.jwt.refresh-token-expire}")
    private long refreshTokenExpire;

    @Value("${third-party.jwt.secret:}")
    private String thirdPartyJwtSecret;

    @Value("${third-party.jwt.access-token-expire:86400}")
    private long thirdPartyAccessTokenExpire;

    private static final long SMS_CODE_EXPIRE = 300; // 验证码有效期5分钟
    private static final long CANCEL_CONFIRM_TOKEN_EXPIRE = 600; // 注销确认凭证有效期10分钟
    private static final int MAX_SMS_ERROR_COUNT = 5; // 最大错误次数
    private static final String CANCEL_CONFIRM_TOKEN_PREFIX = "user:cancel:token:";
    private static final String DEFAULT_SIGNATURE = "生命不息运动不止";
    private static final int DEFAULT_SPORTS_YEARS = 0;
    private static final int DEFAULT_POWER = 5;
    private static final int DEFAULT_SPEED = 5;
    private static final int DEFAULT_SERVE = 5;
    private static final int DEFAULT_VOLLEY = 5;
    private static final int DEFAULT_STAMINA = 5;
    private static final int DEFAULT_MENTAL = 5;
    private static final String DEFAULT_HOME_DISTRICT = "510000";
    private static final int GLOBOX_NO_SEQ_MAX = 9999;
    private static final long GLOBOX_NO_SEQ_EXPIRE_SECONDS = 2 * 24 * 60 * 60;
    private static final String WHITELIST_LOGIN_PASSWORD = "970824"; // 白名单免验证码固定密码
    private static final String WHITELIST_CANCEL_PASSWORD = "970824"; // 白名单注销固定密码

    @Override
    public R<String> sendCaptcha(SendCaptchaRequest request) {
        String phone = request.getPhone();

        // 1. 验证手机号格式（使用Assert）
        Assert.isTrue(PhoneUtils.isValidPhone(phone), UserAuthCode.INVALID_PHONE);

        // 2. 白名单校验
        Assert.isTrue(whitelistService.isInWhitelist(phone), UserAuthCode.NOT_IN_WHITELIST);

        // 3. 频率限制（60秒内只能发送一次）
        boolean canSend = redisService.canSendSms(phone);
        if (!canSend) {
            long waitSeconds = redisService.getSmsRateLimitSeconds(phone);
            String message = waitSeconds > 0
                    ? "验证码发送过于频繁，请在 " + waitSeconds + " 秒后重试"
                    : UserAuthCode.SMS_SEND_TOO_FREQUENT.getMessage();
            throw new GloboxApplicationException(UserAuthCode.SMS_SEND_TOO_FREQUENT.getCode(), message);
        }

        // 4. 生成验证码
        String code = smsService.generateCode();

        // 5. 发送短信
        boolean sendSuccess = smsService.sendCode(phone, code);
        Assert.isTrue(sendSuccess, UserAuthCode.SMS_SEND_TOO_FREQUENT);

        // 6. 保存到Redis（5分钟有效期）
        redisService.saveSmsCode(phone, code, SMS_CODE_EXPIRE);

        // 7. 记录发送频率（60秒限制）
        redisService.recordSmsSent(phone);

        log.info("验证码发送成功：phone={}", phone);
        return R.ok("验证码已发送，请注意查收");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<LoginResponse> phoneLogin(PhoneLoginRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();

        // 1. 验证手机号格式
        Assert.isTrue(PhoneUtils.isValidPhone(phone), UserAuthCode.INVALID_PHONE);

        // 2. 白名单校验（非白名单仍保持原逻辑）
        InternalTestWhitelist whitelist = whitelistService.getWhitelistByPhone(phone);
        boolean inWhitelist = whitelist != null
                && whitelist.getStatus() == InternalTestWhitelist.WhitelistStatus.ACTIVE;

        // 3. 白名单用户可用固定密码或短信验证码
        if (inWhitelist) {
            long errorCount = redisService.getSmsErrorCount(phone);
            Assert.isTrue(errorCount < MAX_SMS_ERROR_COUNT, UserAuthCode.CAPTCHA_ERROR_TOO_MANY);

            String inputCode = code.trim();
            if (WHITELIST_LOGIN_PASSWORD.equals(inputCode)) {
                redisService.clearSmsError(phone);
            } else {
                String savedCode = redisService.getSmsCode(phone);
                Assert.isNotEmpty(savedCode, UserAuthCode.INVALID_CAPTCHA);

                if (!inputCode.equals(savedCode.trim())) {
                    redisService.incrementSmsError(phone);
                    recordLoginLog(null, phone, AuthIdentity.IdentityType.PHONE, false, UserAuthCode.INVALID_CAPTCHA.getMessage());
                    throw new GloboxApplicationException(UserAuthCode.INVALID_CAPTCHA);
                }

                redisService.clearSmsError(phone);
                redisService.deleteSmsCode(phone);
            }
        } else {
            // 3b. 正常验证码登录流程
            long errorCount = redisService.getSmsErrorCount(phone);
            Assert.isTrue(errorCount < MAX_SMS_ERROR_COUNT, UserAuthCode.CAPTCHA_ERROR_TOO_MANY);

            String savedCode = redisService.getSmsCode(phone);
            Assert.isNotEmpty(savedCode, UserAuthCode.INVALID_CAPTCHA);

            if (!code.equals(savedCode)) {
                redisService.incrementSmsError(phone);
                recordLoginLog(null, phone, AuthIdentity.IdentityType.PHONE, false, UserAuthCode.INVALID_CAPTCHA.getMessage());
                throw new GloboxApplicationException(UserAuthCode.INVALID_CAPTCHA);
            }

            redisService.clearSmsError(phone);
            redisService.deleteSmsCode(phone);
        }

        // 6. 查询或创建用户（登录即注册）- 使用统一方法
        LoginUserResult loginResult = loginOrRegisterByIdentity(
                AuthIdentity.IdentityType.PHONE,
                phone,
                new ProfileInit("用户" + phone.substring(phone.length() - 4), null)
        );
        AuthUser authUser = loginResult.authUser();
        boolean created = loginResult.created();
        boolean reactivated = loginResult.reactivated();
        boolean appFirstLogin = loginResult.appFirstLogin();
        boolean isNewUser = created || reactivated || appFirstLogin;

        // 7. 生成JWT Token
        String clientTypeHeader = RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        String clientType = resolveClientTypeForAppLogin(clientTypeHeader);
        log.info("手机号登录客户端类型: clientType={}, headerPresent={}", clientType, StringUtils.hasText(clientTypeHeader));
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", authUser.getRole());
        String accessToken = generateAccessToken(
                authUser.getUserId(),
                claims,
                jwtSecret,
                accessTokenExpire,
                clientType
        );

        String refreshToken = generateRefreshToken(
                authUser.getUserId(),
                claims,
                jwtSecret,
                refreshTokenExpire,
                clientType
        );

        // 8. 保存Refresh Token到Redis
        saveRefreshTokenForClient(authUser.getUserId(), refreshToken, refreshTokenExpire, clientType);
        // 9. 记录登录日志
        recordLoginLog(authUser.getUserId(), phone, AuthIdentity.IdentityType.PHONE, true, null);

        // 10. 注册设备（如果提供了设备信息）
        registerDeviceIfPresent(authUser.getUserId(), authUser.getRole().name(), request.getDeviceInfo());

        // 11. 构建响应（token 为纯 JWT 字符串，调用方自行在 Header 中加 Bearer 前缀）
        LoginResponse response = LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(authUser.getUserId())
                .roles(Collections.singletonList(authUser.getRole().name()))
                .isNewUser(isNewUser)
                .reactivated(reactivated)
                .build();

        log.info("用户登录成功：userId={}, phone={}", authUser.getUserId(), phone);
        return R.ok(response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<WechatLoginResponse> wechatLogin(WechatLoginRequest request) {
        String code = request.getCode();

        // 1. 获取客户端类型
        String clientType = RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        if (!StringUtils.hasText(clientType)) {
            log.error("【微信登录】缺少X-Client-Type请求头，期望值：app 或 third-party-jsapi");
            throw new GloboxApplicationException(UserAuthCode.CLIENT_TYPE_UNSUPPORTED);
        }
        log.info("wechat login start: clientType={}, hasCode={}", clientType, StringUtils.hasText(code));

        // 2. 调用微信API换取openid/unionid
        WechatUserInfo wechatUserInfo = wechatService.getOpenIdAndUnionId(code, clientType);
        String openid = wechatUserInfo.getOpenid();
        String unionid = wechatUserInfo.getUnionid();
        // TODO: DEV ONLY: log openid for debugging, remove before release.
        log.info("DEV ONLY wechat openid={}", openid);
        log.info("wechat unionid={}", unionid);

        // 2. 优先使用unionid，如果没有则使用openid
        String identifier = unionid != null ? unionid : openid;
        log.info("wechat login request: clientType={}, identifier={}, openid={}, unionid={}",
                clientType, identifier, openid, unionid);

        // 3. 查询auth_identity表，判断是否已绑定
        LambdaQueryWrapper<AuthIdentity> identityQuery = new LambdaQueryWrapper<>();
        identityQuery.eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.WECHAT)
                .eq(AuthIdentity::getIdentifier, identifier);
        AuthIdentity identity = authIdentityMapper.selectOne(identityQuery);

        if (identity != null) {
            // 4. 已绑定：直接登录 - 使用统一方法中的逻辑
            LoginUserResult loginResult = loginOrRegisterByIdentity(
                    AuthIdentity.IdentityType.WECHAT,
                    identifier,
                    null
            );
            AuthUser authUser = loginResult.authUser();
            boolean reactivated = loginResult.reactivated();

            boolean isThirdParty = isThirdPartyClient();
            if (!isThirdParty) {
                log.info("app wechat identity hit: identityId={}, userId={}, identifier={}",
                        identity.getIdentityId(), authUser.getUserId(), identifier);
            }

            // 生成JWT Token
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", authUser.getRole());

            String accessToken;
            String refreshToken = null;
            if (isThirdParty) {
                claims.put("clientType", ClientType.THIRD_PARTY_JSAPI.getValue());
                claims.put("openid", openid);  // 只有第三方小程序才加入openid
                if (!StringUtils.hasText(thirdPartyJwtSecret)) {
                    log.error("第三方小程序 JWT secret 未配置");
                    throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
                }
                accessToken = generateAccessToken(
                        authUser.getUserId(),
                        claims,
                        thirdPartyJwtSecret,
                        thirdPartyAccessTokenExpire,
                        clientType
                );
            } else {
                accessToken = generateAccessToken(
                        authUser.getUserId(),
                        claims,
                        jwtSecret,
                        accessTokenExpire,
                        clientType
                );

                refreshToken = generateRefreshToken(
                        authUser.getUserId(),
                        claims,
                        jwtSecret,
                        refreshTokenExpire,
                        clientType
                );
                // 保存Refresh Token到Redis
                saveRefreshTokenForClient(authUser.getUserId(), refreshToken, refreshTokenExpire, clientType);
            }

            // 记录登录日志
            recordLoginLog(authUser.getUserId(), openid, AuthIdentity.IdentityType.WECHAT, true, null);

            // 注册设备（如果提供了设备信息）
            registerDeviceIfPresent(authUser.getUserId(), authUser.getRole().name(), request.getDeviceInfo());

            // 查询用户资料与手机号，用于返回统一 userInfo
            UserProfile userProfile = userProfileMapper.selectById(authUser.getUserId());
            LambdaQueryWrapper<AuthIdentity> phoneIdentityQuery = new LambdaQueryWrapper<>();
            phoneIdentityQuery.eq(AuthIdentity::getUserId, authUser.getUserId())
                    .eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE);
            AuthIdentity phoneIdentity = authIdentityMapper.selectOne(phoneIdentityQuery);

            ThirdPartyLoginResponse.UserInfo userInfo = ThirdPartyLoginResponse.UserInfo.builder()
                    .id(authUser.getUserId())
                    .phone(phoneIdentity != null ? phoneIdentity.getIdentifier() : null)
                    .nickname(userProfile != null ? userProfile.getNickName() : null)
                    .avatarUrl(resolveAvatarUrlForResponse(userProfile != null ? userProfile.getAvatarUrl() : null, clientType))
                    .build();

            // 已绑定直接登录（除非是重新激活的账号或第一次登录 app 的账号）
            boolean isNewUser = reactivated || loginResult.appFirstLogin;

            // 构建响应（token 为纯 JWT 字符串）
            WechatLoginResponse response = WechatLoginResponse.builder()
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .userId(authUser.getUserId())
                    .roles(Collections.singletonList(authUser.getRole().name()))
                    .isNewUser(isNewUser)
                    .needBindPhone(false)
                    .userInfo(userInfo)
                    .nickname(userInfo.getNickname())
                    .avatarUrl(userInfo.getAvatarUrl())
                    .build();

            log.info("微信登录成功：userId={}, openid={}", authUser.getUserId(), openid);
            return R.ok(response);
        } else {
            // 5. 未绑定：判断客户端类型
            boolean isApp = isAppClient();

            if (isApp) {
                log.info("app wechat identity miss: identifier={}, openid={}, unionid={}", identifier, openid, unionid);
                // App端：直接创建新用户并登录 - 使用统一方法
                LoginUserResult loginResult = loginOrRegisterByIdentity(
                        AuthIdentity.IdentityType.WECHAT,
                        identifier,
                        new ProfileInit("微信用户" + identifier.substring(Math.max(0, identifier.length() - 4)), null)
                );
                AuthUser authUser = loginResult.authUser();
                boolean created = loginResult.created();
                boolean reactivated = loginResult.reactivated();
                boolean appFirstLogin = loginResult.appFirstLogin();
                boolean isNewUser = created || reactivated || appFirstLogin;

                boolean isThirdParty = isThirdPartyClient();

                // 生成JWT Token
                Map<String, Object> claims = new HashMap<>();
                claims.put("role", authUser.getRole());

                String accessToken;
                String refreshToken = null;
                if (isThirdParty) {
                    claims.put("clientType", ClientType.THIRD_PARTY_JSAPI.getValue());
                    claims.put("openid", openid);  // 只有第三方小程序才加入openid
                    if (!StringUtils.hasText(thirdPartyJwtSecret)) {
                        log.error("第三方小程序 JWT secret 未配置");
                        throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
                    }
                    accessToken = generateAccessToken(
                            authUser.getUserId(),
                            claims,
                            thirdPartyJwtSecret,
                            thirdPartyAccessTokenExpire,
                            clientType
                    );
                } else {
                    accessToken = generateAccessToken(
                            authUser.getUserId(),
                            claims,
                            jwtSecret,
                            accessTokenExpire,
                            clientType
                    );

                    refreshToken = generateRefreshToken(
                            authUser.getUserId(),
                            claims,
                            jwtSecret,
                            refreshTokenExpire,
                            clientType
                    );
                    saveRefreshTokenForClient(authUser.getUserId(), refreshToken, refreshTokenExpire, clientType);
                }

                // 记录登录日志
                recordLoginLog(authUser.getUserId(), openid, AuthIdentity.IdentityType.WECHAT, true, null);

                // 注册设备
                registerDeviceIfPresent(authUser.getUserId(), authUser.getRole().name(), request.getDeviceInfo());

                // 查询用户资料与手机号
                UserProfile userProfile = userProfileMapper.selectById(authUser.getUserId());
                LambdaQueryWrapper<AuthIdentity> phoneIdentityQuery = new LambdaQueryWrapper<>();
                phoneIdentityQuery.eq(AuthIdentity::getUserId, authUser.getUserId())
                        .eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE);
                AuthIdentity phoneIdentity = authIdentityMapper.selectOne(phoneIdentityQuery);

                ThirdPartyLoginResponse.UserInfo userInfo = ThirdPartyLoginResponse.UserInfo.builder()
                        .id(authUser.getUserId())
                        .phone(phoneIdentity != null ? phoneIdentity.getIdentifier() : null)
                        .nickname(userProfile != null ? userProfile.getNickName() : null)
                        .avatarUrl(resolveAvatarUrlForResponse(userProfile != null ? userProfile.getAvatarUrl() : null, clientType))
                        .build();

                WechatLoginResponse response = WechatLoginResponse.builder()
                        .token(accessToken)
                        .refreshToken(refreshToken)
                        .userId(authUser.getUserId())
                        .roles(Collections.singletonList(authUser.getRole().name()))
                        .isNewUser(isNewUser)
                        .needBindPhone(false)
                        .userInfo(userInfo)
                        .nickname(userInfo.getNickname())
                        .avatarUrl(userInfo.getAvatarUrl())
                        .build();

                log.info("App微信登录成功（新用户）：userId={}, identifier={}, openid={}, unionid={}",
                        authUser.getUserId(), identifier, openid, unionid);
                return R.ok(response);
            } else {
                // 小程序/第三方：生成临时凭证，需要绑定手机号
                String tempToken = UUID.randomUUID().toString();
                // 保存临时凭证，格式：identifier|openid|clientType
                String tempTokenValue = identifier + "|" + openid + "|" + clientType;
                redisService.saveWechatTempToken(tempToken, tempTokenValue, 5); // TTL=5分钟

                WechatLoginResponse response = WechatLoginResponse.builder()
                        .needBindPhone(true)
                        .tempToken(tempToken)
                        .build();

                log.info("微信未绑定，返回临时凭证：openid={}, clientType={}", openid, clientType);
                return R.ok(response);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<LoginResponse> wechatBindPhone(WechatBindPhoneRequest request) {
        String tempToken = request.getTempToken();
        String phone = request.getPhone();
        String code = request.getCode();
        String nickname = request.getNickname();
        String avatarUrl = request.getAvatarUrl();

        // 1. 验证临时凭证并解析
        String tempTokenValue = redisService.getWechatTempToken(tempToken);
        Assert.isNotEmpty(tempTokenValue, UserAuthCode.TEMP_TOKEN_EXPIRED);

        // 解析临时凭证（格式：identifier|openid|clientType）
        String[] parts = tempTokenValue.split("\\|");
        String identifier = parts[0];
        String openid = parts.length > 1 ? parts[1] : null;
        String savedClientType = parts.length > 2 ? parts[2] : null;

        log.info("wechatBindPhone: identifier={}, openid={}, clientType={}", identifier, openid, savedClientType);

        // 2. 验证手机号格式
        Assert.isTrue(PhoneUtils.isValidPhone(phone), UserAuthCode.INVALID_PHONE);

        // 3. 白名单校验
        Assert.isTrue(whitelistService.isInWhitelist(phone), UserAuthCode.NOT_IN_WHITELIST);

        // 4. 验证错误次数检查
        long errorCount = redisService.getSmsErrorCount(phone);
        Assert.isTrue(errorCount < MAX_SMS_ERROR_COUNT, UserAuthCode.CAPTCHA_ERROR_TOO_MANY);

        // 5. 验证码校验
        String storedCode = redisService.getSmsCode(phone);
        Assert.isNotEmpty(storedCode, UserAuthCode.INVALID_CAPTCHA);

        if (!code.equals(storedCode)) {
            // 错误次数+1
            redisService.incrementSmsError(phone);
            // 记录登录失败日志
            recordLoginLog(null, phone, AuthIdentity.IdentityType.WECHAT, false, UserAuthCode.INVALID_CAPTCHA.getMessage());
            throw new GloboxApplicationException(UserAuthCode.INVALID_CAPTCHA);
        }

        // 6. 验证码正确，清除错误计数和验证码
        redisService.clearSmsError(phone);
        redisService.deleteSmsCode(phone);

        // 7. 查询手机号是否已注册，使用统一方法
        LambdaQueryWrapper<AuthIdentity> phoneIdentityQuery = new LambdaQueryWrapper<>();
        phoneIdentityQuery.eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE)
                .eq(AuthIdentity::getIdentifier, phone);
        AuthIdentity phoneIdentity = authIdentityMapper.selectOne(phoneIdentityQuery);

        Long userId;
        boolean isNewUser = false;

        if (phoneIdentity != null) {
            // 7a. 已注册：绑定微信到现有账号，使用统一方法
            LoginUserResult phoneLoginResult = loginOrRegisterByIdentity(
                    AuthIdentity.IdentityType.PHONE,
                    phone,
                    null
            );
            userId = phoneLoginResult.authUser().getUserId();

            // 检查该微信是否已绑定其他账号
            assertIdentityNotBoundToOtherUser(AuthIdentity.IdentityType.WECHAT, identifier, userId);

            // 如果微信未绑定，创建绑定记录
            LambdaQueryWrapper<AuthIdentity> wechatCheckQuery = new LambdaQueryWrapper<>();
            wechatCheckQuery.eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.WECHAT)
                    .eq(AuthIdentity::getIdentifier, identifier);
            AuthIdentity existingWechatIdentity = authIdentityMapper.selectOne(wechatCheckQuery);

            if (existingWechatIdentity == null) {
                AuthIdentity wechatIdentity = new AuthIdentity();
                wechatIdentity.setIdentityId(UUID.randomUUID().toString());
                wechatIdentity.setUserId(userId);
                wechatIdentity.setIdentityType(AuthIdentity.IdentityType.WECHAT);
                wechatIdentity.setIdentifier(identifier);
                wechatIdentity.setCredential(null);
                wechatIdentity.setVerified(true);
                wechatIdentity.setCancelled(false);
                authIdentityMapper.insert(wechatIdentity);
                log.info("小程序手机号登录：userId={}, identifier={}, phone={}", userId, identifier, phone);
            }

            log.info("微信绑定到现有账号：userId={}, phone={}", userId, phone);
        } else {
            // 7b. 未注册：创建新账号，使用统一方法
            isNewUser = true;

            // 使用统一方法创建手机号账号
            String nicknameToUse = (nickname != null && !nickname.trim().isEmpty())
                    ? nickname
                    : "用户" + phone.substring(phone.length() - 4);
            LoginUserResult phoneLoginResult = loginOrRegisterByIdentity(
                    AuthIdentity.IdentityType.PHONE,
                    phone,
                    new ProfileInit(nicknameToUse, avatarUrl)
            );
            userId = phoneLoginResult.authUser().getUserId();

            // 添加微信身份绑定
            AuthIdentity wechatIdentity = new AuthIdentity();
            wechatIdentity.setIdentityId(UUID.randomUUID().toString());
            wechatIdentity.setUserId(userId);
            wechatIdentity.setIdentityType(AuthIdentity.IdentityType.WECHAT);
            wechatIdentity.setIdentifier(identifier);
            wechatIdentity.setCredential(null);
            wechatIdentity.setVerified(true);
            wechatIdentity.setCancelled(false);
            authIdentityMapper.insert(wechatIdentity);
            log.info("小程序手机号登录（新用户）：userId={}, identifier={}, phone={}", userId, identifier, phone);

            log.info("新用户注册成功（微信登录）：userId={}, phone={}", userId, phone);
        }

        // 8. 删除临时凭证
        redisService.deleteWechatTempToken(tempToken);

        // 9. 根据clientType生成JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", AuthUser.UserRole.USER);

        String accessToken;
        String refreshToken;
        boolean isThirdParty = ClientType.THIRD_PARTY_JSAPI.getValue().equalsIgnoreCase(savedClientType);

        if (isThirdParty && StringUtils.hasText(openid)) {
            // 第三方小程序：使用 third-party.jwt.* 配置，并加入openid
            claims.put("clientType", ClientType.THIRD_PARTY_JSAPI.getValue());
            claims.put("openid", openid);
            if (!StringUtils.hasText(thirdPartyJwtSecret)) {
                log.error("第三方小程序 JWT secret 未配置");
                throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
            }
            accessToken = generateAccessToken(
                    userId,
                    claims,
                    thirdPartyJwtSecret,
                    thirdPartyAccessTokenExpire,
                    savedClientType
            );
            // 第三方小程序不生成 refreshToken
            refreshToken = null;
            log.info("第三方小程序绑定手机号成功：userId={}, phone={}, openid={}", userId, phone, openid);
        } else {
            // 普通小程序/App：使用 user.jwt.* 配置
            accessToken = generateAccessToken(
                    userId,
                    claims,
                    jwtSecret,
                    accessTokenExpire,
                    savedClientType
            );

            refreshToken = generateRefreshToken(
                    userId,
                    claims,
                    jwtSecret,
                    refreshTokenExpire,
                    savedClientType
            );

            // 10. 保存Refresh Token到Redis
            saveRefreshTokenForClient(userId, refreshToken, refreshTokenExpire, savedClientType);
        }

        // 11. 记录登录日志
        String loginIdentifier = StringUtils.hasText(openid) ? openid : phone;
        recordLoginLog(userId, loginIdentifier, AuthIdentity.IdentityType.WECHAT, true, null);

        // 12. 注册设备（如果提供了设备信息）
        registerDeviceIfPresent(userId, AuthUser.UserRole.USER.name(), request.getDeviceInfo());

        // 13. 构建响应（token 为纯 JWT 字符串）
        LoginResponse response = LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .roles(Collections.singletonList(AuthUser.UserRole.USER.name()))
                .isNewUser(isNewUser)
                .reactivated(false)
                .build();

        return R.ok(response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<ThirdPartyLoginResponse> wechatPhoneLogin(WechatPhoneLoginRequest request) {
        String wxCode = request.getWxCode();
        String phoneCode = request.getPhoneCode();
        String nickname = request.getNickname();
        String avatarUrl = request.getAvatarUrl();

        // 1. 获取客户端类型
        String clientType = RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        log.info("wechat phone login start: clientType={}, hasWxCode={}, hasPhoneCode={}, profiles={}",
                clientType, StringUtils.hasText(wxCode), StringUtils.hasText(phoneCode),
                Arrays.toString(environment.getActiveProfiles()));

        if (!StringUtils.hasText(clientType)) {
            log.error("【微信手机号登录】缺少X-Client-Type请求头，期望值：app 或 third-party-jsapi");
            throw new GloboxApplicationException(UserAuthCode.CLIENT_TYPE_UNSUPPORTED);
        }

        // 2. 获取openid（用于第三方小程序）
        WechatUserInfo wechatUserInfo = wechatService.getOpenIdAndUnionId(wxCode, clientType);
        String openid = wechatUserInfo.getOpenid();
        log.info("wechat phone login openid={}", openid);

        // 3. 使用phoneCode获取手机号
        String phone = wechatService.getPhoneNumber(wxCode, phoneCode, clientType);

        // 4. 验证手机号格式
        Assert.isTrue(PhoneUtils.isValidPhone(phone), UserAuthCode.INVALID_PHONE);

        // 5. 白名单校验
        Assert.isTrue(whitelistService.isInWhitelist(phone), UserAuthCode.NOT_IN_WHITELIST);

        // 6. 查询手机号是否已注册，使用统一方法
        LambdaQueryWrapper<AuthIdentity> phoneIdentityQuery = new LambdaQueryWrapper<>();
        phoneIdentityQuery.eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE)
                .eq(AuthIdentity::getIdentifier, phone);
        AuthIdentity phoneIdentity = authIdentityMapper.selectOne(phoneIdentityQuery);

        Long userId;
        boolean isNewUser = false;

        if (phoneIdentity != null) {
            // 6a. 已注册：仅使用手机号账号登录
            LoginUserResult phoneLoginResult = loginOrRegisterByIdentity(
                    AuthIdentity.IdentityType.PHONE,
                    phone,
                    null
            );
            userId = phoneLoginResult.authUser().getUserId();
            log.info("第三方小程序手机号登录：使用现有账号：userId={}, phone={}", userId, phone);
        } else {
            // 6b. 未注册：创建手机号账号
            isNewUser = true;

            String nicknameToUse = (nickname != null && !nickname.trim().isEmpty())
                    ? nickname
                    : "用户" + phone.substring(phone.length() - 4);
            LoginUserResult phoneLoginResult = loginOrRegisterByIdentity(
                    AuthIdentity.IdentityType.PHONE,
                    phone,
                    new ProfileInit(nicknameToUse, avatarUrl)
            );
            userId = phoneLoginResult.authUser().getUserId();

            log.info("第三方小程序新用户注册成功（手机号登录）：userId={}, phone={}", userId, phone);
        }

        // 6c. 将 openid 持久化到 PHONE 类型 AuthIdentity 的 credential 字段（明文，不加 hash）
        if (StringUtils.hasText(openid)) {
            LambdaQueryWrapper<AuthIdentity> updateQuery = new LambdaQueryWrapper<>();
            updateQuery.eq(AuthIdentity::getUserId, userId)
                    .eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE)
                    .eq(AuthIdentity::getIdentifier, phone);
            AuthIdentity phoneIdentityToUpdate = authIdentityMapper.selectOne(updateQuery);
            if (phoneIdentityToUpdate != null) {
                phoneIdentityToUpdate.setCredential(openid);
                authIdentityMapper.updateById(phoneIdentityToUpdate);
                log.info("微信手机号登录：已保存 openid 到 credential 字段，userId={}, phone={}", userId, phone);
            }
        }

        // 7. 查询用户资料（用于返回用户信息）
        UserProfile userProfile = userProfileMapper.selectById(userId);

        // 8. 根据clientType生成JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", AuthUser.UserRole.USER);

        String accessToken;
        String refreshToken = null;
        boolean isThirdParty = ClientType.THIRD_PARTY_JSAPI.getValue().equalsIgnoreCase(clientType);

        if (isThirdParty) {
            // 第三方小程序：使用 third-party.jwt.* 配置
            claims.put("clientType", ClientType.THIRD_PARTY_JSAPI.getValue());
            claims.put("openid", openid);  // 只有第三方小程序才加入openid
            if (!StringUtils.hasText(thirdPartyJwtSecret)) {
                log.error("第三方小程序 JWT secret 未配置");
                throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
            }
            accessToken = generateAccessToken(
                    userId,
                    claims,
                    thirdPartyJwtSecret,
                    thirdPartyAccessTokenExpire,
                    clientType
            );
            // 第三方小程序不生成 refreshToken
        } else {
            // App端：使用 user.jwt.* 配置
            accessToken = generateAccessToken(
                    userId,
                    claims,
                    jwtSecret,
                    accessTokenExpire,
                    clientType
            );
            refreshToken = generateRefreshToken(
                    userId,
                    claims,
                    jwtSecret,
                    refreshTokenExpire,
                    clientType
            );
            // 保存Refresh Token到Redis
            saveRefreshTokenForClient(userId, refreshToken, refreshTokenExpire, clientType);
        }

        // 9. 记录登录日志
        recordLoginLog(userId, phone, AuthIdentity.IdentityType.WECHAT, true, null);

        // 10. 构建响应
        ThirdPartyLoginResponse.UserInfo userInfo = ThirdPartyLoginResponse.UserInfo.builder()
                .id(userId)
                .phone(phone)
                .nickname(userProfile != null ? userProfile.getNickName() : null)
                .avatarUrl(resolveAvatarUrlForResponse(userProfile != null ? userProfile.getAvatarUrl() : null, clientType))
                .build();

        ThirdPartyLoginResponse response = ThirdPartyLoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userInfo(userInfo)
                .isNewUser(isNewUser)
                .build();

        log.info("第三方小程序微信手机号登录成功：userId={}, phone={}, isNewUser={}", userId, phone, isNewUser);
        return R.ok(response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> setPassword(SetPasswordRequest request) {
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();

        // 1. 从请求头提取Token
        String authHeader = RequestContextHolder.getHeader("Authorization");
        String token = JwtUtil.extractTokenFromHeader(authHeader);
        Assert.isNotEmpty(token, UserAuthCode.TOKEN_INVALID);

        // 2. 验证Token有效性
        String clientType = RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        Assert.isTrue(JwtUtil.validateToken(token, jwtSecret), UserAuthCode.TOKEN_INVALID);
        assertAppAccessTokenActive(token, clientType, jwtSecret);
        // 3. 从Token中提取userId
        Long userId = Long.parseLong(JwtUtil.getSubject(token, jwtSecret));

        // 4. 校验密码强度
        Assert.isTrue(PasswordUtils.isValidPassword(password), UserAuthCode.PASSWORD_TOO_WEAK);

        // 5. 校验两次密码一致性
        Assert.isTrue(password.equals(confirmPassword), UserAuthCode.PASSWORD_MISMATCH);

        // 6. 查询用户身份
        LambdaQueryWrapper<AuthIdentity> identityQuery = new LambdaQueryWrapper<>();
        identityQuery.eq(AuthIdentity::getUserId, userId)
                .eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE);
        AuthIdentity identity = authIdentityMapper.selectOne(identityQuery);
        Assert.isNotEmpty(identity, UserAuthCode.USER_NOT_EXIST);
        assertIdentityActive(identity);

        AuthUser authUser = authUserMapper.selectById(userId);
        Assert.isNotEmpty(authUser, UserAuthCode.USER_NOT_EXIST);
        assertUserActive(authUser);

        // 7. 如果已存在密码，拒绝重复设置
        if (identity.getCredential() != null && !identity.getCredential().isEmpty()) {
            return R.error(UserAuthCode.PASSWORD_ALREADY_SET);
        }

        // 8. BCrypt加密密码
        String encodedPassword = PasswordUtils.encode(password);

        // 9. 更新密码
        identity.setCredential(encodedPassword);
        authIdentityMapper.updateById(identity);

        // 10. 首次设置密码成功
        log.info("密码设置成功（首次设置）：userId={}", userId);
        return R.ok("密码设置成功，请重新登录");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<LoginResponse> passwordLogin(PasswordLoginRequest request) {
        String phone = request.getPhone();
        String password = request.getPassword();

        // 1. 验证手机号格式
        Assert.isTrue(PhoneUtils.isValidPhone(phone), UserAuthCode.INVALID_PHONE);

        // 2. 检查密码错误次数（防止暴力破解）
        long errorCount = redisService.getPasswordErrorCount(phone);
        Assert.isTrue(errorCount < MAX_SMS_ERROR_COUNT, UserAuthCode.CAPTCHA_ERROR_TOO_MANY);

        // 3. 查询用户身份
        LambdaQueryWrapper<AuthIdentity> identityQuery = new LambdaQueryWrapper<>();
        identityQuery.eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE)
                .eq(AuthIdentity::getIdentifier, phone);
        AuthIdentity identity = authIdentityMapper.selectOne(identityQuery);

        // 4. 账号枚举防护：用户不存在或密码错误时，统一返回"手机号或密码错误"
        // 内部日志仍记录真实原因
        if (identity == null) {
            // 用户不存在，记录日志但统一返回错误信息
            recordLoginLog(null, phone, AuthIdentity.IdentityType.PHONE, false,
                    UserAuthCode.USER_NOT_EXIST.getMessage());
            redisService.incrementPasswordError(phone);
            throw new GloboxApplicationException(UserAuthCode.LOGIN_FAILED);
        }

        // 5. 检查是否已设置密码
        if (identity.getCredential() == null || identity.getCredential().isEmpty()) {
            // 未设置密码，记录日志但统一返回错误信息
            recordLoginLog(identity.getUserId(), phone, AuthIdentity.IdentityType.PHONE, false,
                    UserAuthCode.PASSWORD_NOT_SET.getMessage());
            redisService.incrementPasswordError(phone);
            throw new GloboxApplicationException(UserAuthCode.LOGIN_FAILED);
        }

        // 6. BCrypt验证密码
        boolean matches = PasswordUtils.matches(password, identity.getCredential());

        if (!matches) {
            // 密码错误，记录失败日志并增加错误计数
            recordLoginLog(identity.getUserId(), phone, AuthIdentity.IdentityType.PHONE, false,
                    UserAuthCode.PASSWORD_WRONG.getMessage());
            redisService.incrementPasswordError(phone);
            throw new GloboxApplicationException(UserAuthCode.LOGIN_FAILED);
        }

        // 7. 密码正确，清除错误计数
        redisService.clearPasswordError(phone);

        // 8. 查询用户信息（并处理注销重用）
        AuthUser authUser = ensureUserActiveForLogin(identity, buildDefaultNicknameForPhone(phone)).authUser();
        String clientTypeHeader = RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        String clientType = resolveClientTypeForAppLogin(clientTypeHeader);
        log.info("密码登录客户端类型: clientType={}, headerPresent={}", clientType, StringUtils.hasText(clientTypeHeader));
        // 9. 生成JWT双Token（完全复用phoneLogin的逻辑）
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", authUser.getRole());

        String accessToken = generateAccessToken(
                authUser.getUserId(),
                claims,
                jwtSecret,
                accessTokenExpire,
                clientType
        );

        String refreshToken = generateRefreshToken(
                authUser.getUserId(),
                claims,
                jwtSecret,
                refreshTokenExpire,
                clientType
        );

        // 10. 保存Refresh Token到Redis
        saveRefreshTokenForClient(authUser.getUserId(), refreshToken, refreshTokenExpire, clientType);
        // 11. 记录登录日志
        recordLoginLog(authUser.getUserId(), phone, AuthIdentity.IdentityType.PHONE, true, null);

        // 12. 注册设备（如果提供了设备信息）
        registerDeviceIfPresent(authUser.getUserId(), authUser.getRole().name(), request.getDeviceInfo());

        // 13. 构建响应（token 为纯 JWT 字符串）
        LoginResponse response = LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(authUser.getUserId())
                .roles(Collections.singletonList(authUser.getRole().name()))
                .isNewUser(false)
                .reactivated(false)
                .build();

        log.info("密码登录成功：userId={}, phone={}", authUser.getUserId(), phone);
        return R.ok(response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> resetPassword(ResetPasswordRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        // 1. 验证手机号格式
        Assert.isTrue(PhoneUtils.isValidPhone(phone), UserAuthCode.INVALID_PHONE);

        // 2. 复用验证码校验逻辑（完全参考phoneLogin的实现）
        long errorCount = redisService.getSmsErrorCount(phone);
        Assert.isTrue(errorCount < MAX_SMS_ERROR_COUNT, UserAuthCode.CAPTCHA_ERROR_TOO_MANY);

        String savedCode = redisService.getSmsCode(phone);
        Assert.isNotEmpty(savedCode, UserAuthCode.INVALID_CAPTCHA);

        if (!code.equals(savedCode)) {
            // 错误次数+1
            redisService.incrementSmsError(phone);
            // 记录登录失败日志
            recordLoginLog(null, phone, AuthIdentity.IdentityType.PHONE, false,
                    UserAuthCode.INVALID_CAPTCHA.getMessage());
            throw new GloboxApplicationException(UserAuthCode.INVALID_CAPTCHA);
        }

        // 验证码正确，清除错误计数和验证码
        redisService.clearSmsError(phone);
        redisService.deleteSmsCode(phone);

        // 3. 校验密码强度
        Assert.isTrue(PasswordUtils.isValidPassword(newPassword), UserAuthCode.PASSWORD_TOO_WEAK);

        // 4. 校验两次密码一致性
        Assert.isTrue(newPassword.equals(confirmPassword), UserAuthCode.PASSWORD_MISMATCH);

        // 5. 查询用户身份
        LambdaQueryWrapper<AuthIdentity> identityQuery = new LambdaQueryWrapper<>();
        identityQuery.eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE)
                .eq(AuthIdentity::getIdentifier, phone);
        AuthIdentity identity = authIdentityMapper.selectOne(identityQuery);

        // 6. 检查用户是否存在
        Assert.isNotEmpty(identity, UserAuthCode.USER_NOT_EXIST);
        assertIdentityActive(identity);

        AuthUser authUser = authUserMapper.selectById(identity.getUserId());
        Assert.isNotEmpty(authUser, UserAuthCode.USER_NOT_EXIST);
        assertUserActive(authUser);

        // 7. BCrypt加密新密码
        String encodedPassword = PasswordUtils.encode(newPassword);

        // 8. 更新密码
        identity.setCredential(encodedPassword);
        authIdentityMapper.updateById(identity);

        // 9. 清除该用户所有Refresh Token（强制重新登录）
        redisService.deleteAllRefreshTokens(identity.getUserId());

        log.info("密码重置成功：userId={}, phone={}", identity.getUserId(), phone);
        return R.ok("密码重置成功，请用新密码重新登录");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> verifyCancelAccount(CancelAccountRequest request) {
        Long userId = RequestContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        Assert.isNotEmpty(userId, UserAuthCode.MISSING_USER_ID_HEADER);

        String phone = request.getPhone();
        String code = request.getCode();

        // 1. 验证手机号格式
        Assert.isTrue(PhoneUtils.isValidPhone(phone), UserAuthCode.INVALID_PHONE);

        InternalTestWhitelist whitelist = whitelistService.getWhitelistByPhone(phone);
        boolean inWhitelist = whitelist != null
                && whitelist.getStatus() == InternalTestWhitelist.WhitelistStatus.ACTIVE;
        if (inWhitelist) {
            long errorCount = redisService.getSmsErrorCount(phone);
            Assert.isTrue(errorCount < MAX_SMS_ERROR_COUNT, UserAuthCode.CAPTCHA_ERROR_TOO_MANY);
            String inputCode = code.trim();
            if (WHITELIST_CANCEL_PASSWORD.equals(inputCode)) {
                redisService.clearSmsError(phone);
            } else {
                String savedCode = redisService.getSmsCode(phone);
                Assert.isNotEmpty(savedCode, UserAuthCode.INVALID_CAPTCHA);

                if (!inputCode.equals(savedCode.trim())) {
                    redisService.incrementSmsError(phone);
                    recordLoginLog(userId, phone, AuthIdentity.IdentityType.PHONE, false,
                            UserAuthCode.INVALID_CAPTCHA.getMessage());
                    throw new GloboxApplicationException(UserAuthCode.INVALID_CAPTCHA);
                }

                redisService.clearSmsError(phone);
                redisService.deleteSmsCode(phone);
            }
        } else {
            // 2. 验证码校验（沿用 resetPassword 逻辑）
            long errorCount = redisService.getSmsErrorCount(phone);
            Assert.isTrue(errorCount < MAX_SMS_ERROR_COUNT, UserAuthCode.CAPTCHA_ERROR_TOO_MANY);

            String savedCode = redisService.getSmsCode(phone);
            Assert.isNotEmpty(savedCode, UserAuthCode.INVALID_CAPTCHA);

            String inputCode = code.trim();
            if (!inputCode.equals(savedCode.trim())) {
                redisService.incrementSmsError(phone);
                recordLoginLog(userId, phone, AuthIdentity.IdentityType.PHONE, false,
                        UserAuthCode.INVALID_CAPTCHA.getMessage());
                throw new GloboxApplicationException(UserAuthCode.INVALID_CAPTCHA);
            }

            redisService.clearSmsError(phone);
            redisService.deleteSmsCode(phone);
        }

        // 3. 校验手机号归属
        LambdaQueryWrapper<AuthIdentity> identityQuery = new LambdaQueryWrapper<>();
        identityQuery.eq(AuthIdentity::getUserId, userId)
                .eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE)
                .eq(AuthIdentity::getIdentifier, phone);
        AuthIdentity identity = authIdentityMapper.selectOne(identityQuery);
        Assert.isNotEmpty(identity, UserAuthCode.USER_NOT_EXIST);
        assertIdentityActive(identity);

        AuthUser authUser = authUserMapper.selectById(userId);
        Assert.isNotEmpty(authUser, UserAuthCode.USER_NOT_EXIST);
        assertUserActive(authUser);

        String cancelToken = UUID.randomUUID().toString();
        redisService.set(CANCEL_CONFIRM_TOKEN_PREFIX + cancelToken,
                userId + ":" + phone,
                CANCEL_CONFIRM_TOKEN_EXPIRE);

        log.info("账号注销校验通过：userId={}", userId);
        return R.ok(cancelToken);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> confirmCancelAccount(CancelAccountConfirmRequest request) {
        Long userId = RequestContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        Assert.isNotEmpty(userId, UserAuthCode.MISSING_USER_ID_HEADER);

        String cancelToken = request.getCancelToken();
        String tokenValue = redisService.get(CANCEL_CONFIRM_TOKEN_PREFIX + cancelToken);
        if (!StringUtils.hasText(tokenValue)) {
            throw new GloboxApplicationException(UserAuthCode.CANCEL_CONFIRM_EXPIRED);
        }

        String[] parts = tokenValue.split(":", 2);
        Long tokenUserId = Long.parseLong(parts[0]);
        String phone = parts.length > 1 ? parts[1] : null;
        Assert.isTrue(userId.equals(tokenUserId), UserAuthCode.INVALID_PARAM);

        AuthUser authUser = authUserMapper.selectById(userId);
        Assert.isNotEmpty(authUser, UserAuthCode.USER_NOT_EXIST);
        if (Boolean.TRUE.equals(authUser.getCancelled())) {
            redisService.delete(CANCEL_CONFIRM_TOKEN_PREFIX + cancelToken);
            return R.ok("账号已注销");
        }
        assertUserActive(authUser);

        RpcResult<Void> orderCheckResult = orderForUserDubboService.checkOrderStatusBeforeUserCancel(userId);
        Assert.rpcResultOk(orderCheckResult);

        // 标记账号注销
        authUserMapper.update(null, new LambdaUpdateWrapper<AuthUser>()
                .eq(AuthUser::getUserId, userId)
                .set(AuthUser::getCancelled, true));
        authIdentityMapper.update(null, new LambdaUpdateWrapper<AuthIdentity>()
                .eq(AuthIdentity::getUserId, userId)
                .set(AuthIdentity::getCancelled, true));

        // 初始化用户资料
        resetUserProfileForCancellation(userId);

        // 强制下线
        redisService.deleteAllRefreshTokens(userId);
        redisService.delete(CANCEL_CONFIRM_TOKEN_PREFIX + cancelToken);

        log.info("账号注销成功：userId={}, phone={}", userId, phone);
        return R.ok("账号注销成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> changePassword(ChangePasswordRequest request) {
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        // 1. 从请求头提取Token
        String authHeader = RequestContextHolder.getHeader("Authorization");
        String token = JwtUtil.extractTokenFromHeader(authHeader);
        Assert.isNotEmpty(token, UserAuthCode.TOKEN_INVALID);

        // 2. 验证Token有效性
        String clientType = resolveClientTypeForAppLogin(
                RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE));
        Assert.isTrue(JwtUtil.validateToken(token, jwtSecret), UserAuthCode.TOKEN_INVALID);
        assertAppAccessTokenActive(token, clientType, jwtSecret);

        // 3. 从Token中提取userId
        Long userId = Long.parseLong(JwtUtil.getSubject(token, jwtSecret));

        // 4. 查询用户身份
        LambdaQueryWrapper<AuthIdentity> identityQuery = new LambdaQueryWrapper<>();
        identityQuery.eq(AuthIdentity::getUserId, userId)
                .eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE);
        AuthIdentity identity = authIdentityMapper.selectOne(identityQuery);
        Assert.isNotEmpty(identity, UserAuthCode.USER_NOT_EXIST);
        assertIdentityActive(identity);

        AuthUser authUser = authUserMapper.selectById(userId);
        Assert.isNotEmpty(authUser, UserAuthCode.USER_NOT_EXIST);
        assertUserActive(authUser);

        // 5. 检查是否已设置密码
        Assert.isNotEmpty(identity.getCredential(), UserAuthCode.PASSWORD_NOT_SET);

        // 6. 验证旧密码是否正确（BCrypt）
        boolean matches = PasswordUtils.matches(oldPassword, identity.getCredential());
        if (!matches) {
            // 旧密码错误，记录失败日志
            recordLoginLog(userId, identity.getIdentifier(), AuthIdentity.IdentityType.PHONE, false,
                    UserAuthCode.PASSWORD_WRONG.getMessage());
            throw new GloboxApplicationException(UserAuthCode.PASSWORD_WRONG);
        }

        // 7. 校验新密码强度
        Assert.isTrue(PasswordUtils.isValidPassword(newPassword), UserAuthCode.PASSWORD_TOO_WEAK);

        // 8. 校验两次密码一致性
        Assert.isTrue(newPassword.equals(confirmPassword), UserAuthCode.PASSWORD_MISMATCH);

        // 9. BCrypt加密新密码
        String encodedPassword = PasswordUtils.encode(newPassword);

        // 10. 更新密码
        identity.setCredential(encodedPassword);
        authIdentityMapper.updateById(identity);

        // 11. 清除该用户所有Refresh Token（强制重新登录）
        redisService.deleteAllRefreshTokens(userId);

        log.info("密码修改成功，已清除所有Refresh Token：userId={}", userId);
        return R.ok("密码修改成功，请用新密码重新登录");
    }

    @Override
    public R<LoginResponse> refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        // 客户端类型优先取请求头，缺失时用 refreshToken 中的 claim 回填
        String clientType = RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);

        // 1. 校验 JWT 签名和过期时间
        if (!JwtUtil.validateToken(refreshToken, jwtSecret)) {
            throw new GloboxApplicationException(UserAuthCode.REFRESH_TOKEN_INVALID);
        }
        clientType = resolveClientTypeFromRefreshToken(clientType, refreshToken);

        // 2. 校验 Redis 中是否有效（是否已被吊销）
        if (!redisService.isRefreshTokenValid(refreshToken)) {
            throw new GloboxApplicationException(UserAuthCode.REFRESH_TOKEN_INVALID);
        }

        // 3. 解析 userId
        String userIdStr = JwtUtil.getSubject(refreshToken, jwtSecret);
        Long userId = Long.parseLong(userIdStr);

        // 4. 查询用户信息获取最新角色
        LambdaQueryWrapper<AuthUser> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(AuthUser::getUserId, userId);
        AuthUser authUser = authUserMapper.selectOne(userQuery);
        Assert.isNotEmpty(authUser, UserAuthCode.USER_NOT_EXIST);
        assertUserActive(authUser);

        // 5. 删除旧 refreshToken（token 旋转：旧 token 立即失效）
        redisService.deleteRefreshToken(refreshToken, jwtSecret);

        // 6. 生成新的 access token 和 refresh token
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", authUser.getRole());

        String newAccessToken = generateAccessToken(
                userId,
                claims,
                jwtSecret,
                accessTokenExpire,
                clientType
        );

        String newRefreshToken = generateRefreshToken(
                userId,
                claims,
                jwtSecret,
                refreshTokenExpire,
                clientType
        );

        // 7. 保存新 refresh token 到 Redis
        saveRefreshTokenForClient(userId, newRefreshToken, refreshTokenExpire, clientType);

        // 8. 构建响应
        LoginResponse response = LoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(userId)
                .roles(Collections.singletonList(authUser.getRole().name()))
                .isNewUser(false)
                .reactivated(false)
                .build();

        log.info("Token刷新成功：userId={}", userId);
        return R.ok(response);
    }

    /**
     * 记录登录日志（时间字段由数据库自动填充）
     *
     */
    @Override
    public R<Void> logout() {
        String userIdHeader = RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_USER_ID);
        String clientType = RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        Assert.isNotEmpty(userIdHeader, UserAuthCode.MISSING_USER_ID_HEADER);
        Long userId = Long.parseLong(userIdHeader);
        // 仅 App 端执行单端登出，避免影响其他端
        if (StringUtils.hasText(clientType) && clientType.equalsIgnoreCase(ClientType.APP.getValue())) {
            redisService.deleteAccessTokenJti(userId, clientType);
            redisService.deleteRefreshTokensByClientType(userId, clientType);
            log.info("app logout success: userId={}, clientType={}", userId, clientType);
            return R.ok();
        }
        log.info("logout ignored (non-app): userId={}, clientType={}", userId, clientType);
        return R.ok();
    }

    private void recordLoginLog(Long userId, String identifier, AuthIdentity.IdentityType identityType,
                                boolean success, String failReason) {
        UserLoginRecord record = new UserLoginRecord();
        record.setUserId(userId);
        // 审计日志仅内部使用，保留原始标识
        record.setIdentifierMasked(identifier);
        record.setIdentityType(identityType);
        record.setResult(success ? UserLoginRecord.LoginResult.SUCCESS : UserLoginRecord.LoginResult.FAIL);
        record.setFailReason(failReason);
        record.setIp(HttpRequestUtils.getRealIp());
        record.setUserAgent(HttpRequestUtils.getUserAgent());

        userLoginRecordMapper.insert(record);
    }

    /**
     * 注册设备（登录成功后调用）
     *
     * @param userId 用户ID
     * @param userType 用户角色类型（枚举字符串值）
     * @param deviceInfo 设备信息
     */
    private void registerDeviceIfPresent(Long userId, String userType, DeviceInfo deviceInfo) {
        if (deviceInfo == null || deviceInfo.getDeviceId() == null || deviceInfo.getDeviceToken() == null) {
            log.debug("设备信息不完整，跳过设备注册: userId={}", userId);
            return;
        }

        try {
            DeviceRegisterRequest request = DeviceRegisterRequest.builder()
                    .userId(userId)
                    .userType(userType)
                    .deviceId(deviceInfo.getDeviceId())
                    .deviceToken(deviceInfo.getDeviceToken())
                    .deviceModel(deviceInfo.getDeviceModel())
                    .deviceOs(deviceInfo.getDeviceOs())
                    .appVersion(deviceInfo.getAppVersion())
                    .build();
            request.setUserId(userId);
            userDeviceService.registerDevice(request);
            log.info("设备注册成功: userId={}, userType={}, deviceId={}", userId, userType, deviceInfo.getDeviceId());
        } catch (Exception e) {
            // 设备注册失败不影响登录流程
            log.error("设备注册失败: userId={}, deviceId={}", userId, deviceInfo.getDeviceId(), e);
        }
    }

    /**
     * 创建微信用户并登录（App端，无需绑定手机号）
     *
     * @param identifier 微信标识（unionid或openid）
     * @param openid 微信openid
     * @param deviceInfo 设备信息
     * @param clientType 客户端类型
     * @return 登录响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<WechatLoginResponse> createWechatUserAndLogin(String identifier, String openid,
                                                            DeviceInfo deviceInfo, String clientType) {
        log.info("wechat login create flow: clientType={}, identifier={}, openid={}", clientType, identifier, openid);
        // 1. 检查该微信是否已绑定其他账号（防止并发创建）
        LambdaQueryWrapper<AuthIdentity> wechatCheckQuery = new LambdaQueryWrapper<>();
        wechatCheckQuery.eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.WECHAT)
                .eq(AuthIdentity::getIdentifier, identifier);
        AuthIdentity existingWechatIdentity = authIdentityMapper.selectOne(wechatCheckQuery);
        if (existingWechatIdentity != null) {
            // 如果已存在，走已绑定流程
            log.info("wechat identity exists: identityId={}, userId={}, identifier={}",
                    existingWechatIdentity.getIdentityId(), existingWechatIdentity.getUserId(), identifier);
            AuthUser authUser = ensureUserActiveForLogin(existingWechatIdentity,
                    buildDefaultNicknameForWechat(existingWechatIdentity.getUserId())).authUser();

            boolean isThirdParty = isThirdPartyClient();

            Map<String, Object> claims = new HashMap<>();
            claims.put("role", authUser.getRole());

            String accessToken;
            String refreshToken = null;
            if (isThirdParty) {
                claims.put("clientType", ClientType.THIRD_PARTY_JSAPI.getValue());
                claims.put("openid", openid);  // 只有第三方小程序才加入openid
                if (!StringUtils.hasText(thirdPartyJwtSecret)) {
                    log.error("第三方小程序 JWT secret 未配置");
                    throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
                }
                accessToken = generateAccessToken(
                        authUser.getUserId(),
                        claims,
                        thirdPartyJwtSecret,
                        thirdPartyAccessTokenExpire,
                        clientType
                );
            } else {
                accessToken = generateAccessToken(
                        authUser.getUserId(),
                        claims,
                        jwtSecret,
                        accessTokenExpire,
                        clientType
                );

                refreshToken = generateRefreshToken(
                        authUser.getUserId(),
                        claims,
                        jwtSecret,
                        refreshTokenExpire,
                        clientType
                );
                saveRefreshTokenForClient(authUser.getUserId(), refreshToken, refreshTokenExpire, clientType);
            }

            recordLoginLog(authUser.getUserId(), openid, AuthIdentity.IdentityType.WECHAT, true, null);
            registerDeviceIfPresent(authUser.getUserId(), authUser.getRole().name(), deviceInfo);

            UserProfile userProfile = userProfileMapper.selectById(authUser.getUserId());
            LambdaQueryWrapper<AuthIdentity> phoneIdentityQuery = new LambdaQueryWrapper<>();
            phoneIdentityQuery.eq(AuthIdentity::getUserId, authUser.getUserId())
                    .eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE);
            AuthIdentity phoneIdentity = authIdentityMapper.selectOne(phoneIdentityQuery);

            ThirdPartyLoginResponse.UserInfo userInfo = ThirdPartyLoginResponse.UserInfo.builder()
                    .id(authUser.getUserId())
                    .phone(phoneIdentity != null ? phoneIdentity.getIdentifier() : null)
                    .nickname(userProfile != null ? userProfile.getNickName() : null)
                    .avatarUrl(resolveAvatarUrlForResponse(userProfile != null ? userProfile.getAvatarUrl() : null, clientType))
                    .build();

            WechatLoginResponse response = WechatLoginResponse.builder()
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .userId(authUser.getUserId())
                    .roles(Collections.singletonList(authUser.getRole().name()))
                    .isNewUser(false)
                    .needBindPhone(false)
                    .userInfo(userInfo)
                    .nickname(userInfo.getNickname())
                    .avatarUrl(userInfo.getAvatarUrl())
                    .build();

            return R.ok(response);
        }

        // 2. 创建新用户
        boolean isNewUser = true;

        // 创建auth_user
        AuthUser newUser = new AuthUser();
        newUser.setRole(AuthUser.UserRole.USER);
        newUser.setStatus(AuthUser.UserStatus.ACTIVE);
        newUser.setCancelled(false);
        int insertResult = authUserMapper.insert(newUser);
        Assert.isTrue(insertResult > 0, UserAuthCode.USER_NOT_EXIST);
        Long userId = newUser.getUserId();
        Assert.isNotEmpty(userId, UserAuthCode.USER_NOT_EXIST);

        // 创建auth_identity（WECHAT类型）
        AuthIdentity wechatIdentity = new AuthIdentity();
        wechatIdentity.setIdentityId(UUID.randomUUID().toString());
        wechatIdentity.setUserId(userId);
        wechatIdentity.setIdentityType(AuthIdentity.IdentityType.WECHAT);
        wechatIdentity.setIdentifier(identifier);
        wechatIdentity.setCredential(null);
        wechatIdentity.setVerified(true);
        wechatIdentity.setCancelled(false);
        authIdentityMapper.insert(wechatIdentity);
        log.info("wechat identity created: identityId={}, userId={}, identifier={}",
                wechatIdentity.getIdentityId(), userId, identifier);

        // 创建user_profile，使用默认昵称
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        // 生成默认昵称：微信用户 + 用户ID后4位
        String defaultNickname = "微信用户" + String.format("%04d", userId % 10000);
        profile.setNickName(defaultNickname);
        profile.setAvatarUrl(normalizeAvatarForStorage(null));
        applyDefaultProfileValues(profile);
        userProfileMapper.insert(profile);

        // 导入腾讯IM账号（失败不影响注册流程）
        importTencentIMAccount(userId, profile.getNickName(), profile.getAvatarUrl());

        log.info("新用户注册成功（App微信登录）：userId={}, identifier={}", userId, identifier);

        // 3. 生成JWT Token
        boolean isThirdParty = ClientType.THIRD_PARTY_JSAPI.getValue().equalsIgnoreCase(clientType);
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", AuthUser.UserRole.USER);

        String accessToken;
        String refreshToken = null;
        if (isThirdParty) {
            claims.put("clientType", ClientType.THIRD_PARTY_JSAPI.getValue());
            claims.put("openid", openid);  // 只有第三方小程序才加入openid
            if (!StringUtils.hasText(thirdPartyJwtSecret)) {
                log.error("第三方小程序 JWT secret 未配置");
                throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
            }
            accessToken = generateAccessToken(
                    userId,
                    claims,
                    thirdPartyJwtSecret,
                    thirdPartyAccessTokenExpire,
                    clientType
            );
        } else {
            accessToken = generateAccessToken(
                    userId,
                    claims,
                    jwtSecret,
                    accessTokenExpire,
                    clientType
            );

            refreshToken = generateRefreshToken(
                    userId,
                    claims,
                    jwtSecret,
                    refreshTokenExpire,
                    clientType
            );
            // 保存Refresh Token到Redis
            saveRefreshTokenForClient(userId, refreshToken, refreshTokenExpire, clientType);
        }

        // 4. 记录登录日志
        recordLoginLog(userId, openid, AuthIdentity.IdentityType.WECHAT, true, null);

        // 5. 注册设备（如果提供了设备信息）
        registerDeviceIfPresent(userId, AuthUser.UserRole.USER.name(), deviceInfo);

        // 6. 构建响应
        ThirdPartyLoginResponse.UserInfo userInfo = ThirdPartyLoginResponse.UserInfo.builder()
                .id(userId)
                .phone(null) // App端新用户未绑定手机号
                .nickname(profile.getNickName())
                .avatarUrl(resolveAvatarUrlForResponse(profile.getAvatarUrl(), clientType))
                .build();

        WechatLoginResponse response = WechatLoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .roles(Collections.singletonList(AuthUser.UserRole.USER.name()))
                .isNewUser(isNewUser)
                .needBindPhone(false) // App端新用户不需要强制绑定手机号
                .userInfo(userInfo)
                .nickname(userInfo.getNickname())
                .avatarUrl(userInfo.getAvatarUrl())
                .build();

        log.info("App微信登录成功（新用户）：userId={}, openid={}", userId, openid);
        return R.ok(response);
    }

    /**
     * 导入腾讯IM账号
     * 失败不影响注册登录流程，仅记录日志
     *
     * @param userId 用户ID
     * @param userName 用户昵称
     * @param faceUrl 用户头像URL
     */
    private void importTencentIMAccount(Long userId, String userName, String faceUrl) {
        try {
            String userIdStr = String.valueOf(userId);
            log.info("IM账号导入开始: userId={}, nickName={}", userId, userName);

            RpcResult<Void> importResult = chatDubboService.accountImport(userIdStr, userName, faceUrl);
            Assert.rpcResultOk(importResult);

            log.info("IM账号导入成功: userId={}, nickName={}", userId, userName);
        } catch (Exception e) {
            log.error("IM账号导入异常: userId={}, nickName={}, error={}", userId, userName, e.getMessage(), e);
            // 异常不影响注册登录流程，仅记录日志
        }
    }

    private void assertUserActive(AuthUser authUser) {
        if (Boolean.TRUE.equals(authUser.getCancelled())) {
            throw new GloboxApplicationException(UserAuthCode.USER_ACCOUNT_CANCELLED);
        }
        if (authUser.getStatus() == AuthUser.UserStatus.DISABLED) {
            throw new GloboxApplicationException(UserAuthCode.USER_ACCOUNT_DISABLED);
        }
    }

    private void assertIdentityActive(AuthIdentity identity) {
        if (Boolean.TRUE.equals(identity.getCancelled())) {
            throw new GloboxApplicationException(UserAuthCode.USER_ACCOUNT_CANCELLED);
        }
    }

    private void resetUserProfileForCancellation(Long userId) {
        LambdaUpdateWrapper<UserProfile> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserProfile::getUserId, userId)
                .set(UserProfile::getGloboxNo, "")
                .set(UserProfile::getLastGloboxNoChangedAt, null)
                .set(UserProfile::getAvatarUrl, normalizeAvatarForStorage(null))
                .set(UserProfile::getPortraitUrl, null)
                .set(UserProfile::getNickName, "已注销用户")
                .set(UserProfile::getSignature, DEFAULT_SIGNATURE)
                .set(UserProfile::getGender, GenderEnum.OTHER)
                .set(UserProfile::getSportsStartYear, DEFAULT_SPORTS_YEARS)
                .set(UserProfile::getNtrp, BigDecimal.ZERO)
                .set(UserProfile::getPreferredHand, UserProfile.PreferredHand.RIGHT)
                .set(UserProfile::getHomeDistrict, DEFAULT_HOME_DISTRICT)
                .set(UserProfile::getPower, DEFAULT_POWER)
                .set(UserProfile::getSpeed, DEFAULT_SPEED)
                .set(UserProfile::getServe, DEFAULT_SERVE)
                .set(UserProfile::getVolley, DEFAULT_VOLLEY)
                .set(UserProfile::getStamina, DEFAULT_STAMINA)
                .set(UserProfile::getMental, DEFAULT_MENTAL)
                .set(UserProfile::getCancelled, true);
        userProfileMapper.update(null, updateWrapper);
        cancelUserProfileRelations(userId);
    }

    /**
     * 登录用户结果
     *
     * @param authUser    用户对象
     * @param created     是否为新创建的用户
     * @param reactivated 是否为重新激活的用户
     * @param appFirstLogin 是否为第一次登录 app 的用户
     */
    private record LoginUserResult(AuthUser authUser, boolean created, boolean reactivated, boolean appFirstLogin) {
    }

    /**
     * Profile初始化数据（用于统一登录流程）
     */
    private record ProfileInit(String nickname, String avatarUrl) {
    }

    /**
     * 统一登录或注册流程
     * 支持多种身份类型（手机号、微信、Apple等）
     *
     * @param type 身份类型
     * @param identifier 标识符（手机号/unionid/openid/appleId）
     * @param profileInit Profile初始化数据（可为null，使用默认值）
     * @return LoginUserResult 包含用户信息和是否重新激活标志
     */
    private LoginUserResult loginOrRegisterByIdentity(AuthIdentity.IdentityType type,
                                                      String identifier,
                                                      ProfileInit profileInit) {
        // 1. 查询auth_identity，判断该身份是否已注册
        LambdaQueryWrapper<AuthIdentity> identityQuery = new LambdaQueryWrapper<>();
        identityQuery.eq(AuthIdentity::getIdentityType, type)
                .eq(AuthIdentity::getIdentifier, identifier);
        AuthIdentity identity = authIdentityMapper.selectOne(identityQuery);

        if (identity != null) {
            // 已注册，确保用户状态正常并返回
            log.info("身份命中：identityId={}, userId={}, type={}, identifier={}",
                    identity.getIdentityId(), identity.getUserId(), type, identifier);
            String defaultNickname = buildDefaultNickname(type, identifier, identity.getUserId());
            return ensureUserActiveForLogin(identity, defaultNickname);
        }

        // 2. 未注册，创建新用户
        AuthUser newUser = new AuthUser();
        newUser.setRole(AuthUser.UserRole.USER);
        newUser.setStatus(AuthUser.UserStatus.ACTIVE);
        newUser.setCancelled(false);
        if(isAppClient()) {
            newUser.setAppFirstLogin(false);
        }
        int insertResult = authUserMapper.insert(newUser);
        Assert.isTrue(insertResult > 0, UserAuthCode.USER_NOT_EXIST);
        Long userId = newUser.getUserId();
        Assert.isNotEmpty(userId, UserAuthCode.USER_NOT_EXIST);

        // 3. 创建身份记录
        AuthIdentity newIdentity = new AuthIdentity();
        newIdentity.setIdentityId(UUID.randomUUID().toString());
        newIdentity.setUserId(userId);
        newIdentity.setIdentityType(type);
        newIdentity.setIdentifier(identifier);
        newIdentity.setCredential(null);
        newIdentity.setVerified(true);
        newIdentity.setCancelled(false);
        authIdentityMapper.insert(newIdentity);
        log.info("身份创建：identityId={}, userId={}, type={}, identifier={}",
                newIdentity.getIdentityId(), userId, type, identifier);

        // 4. 创建用户资料
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);

        // 使用提供的nickname和avatarUrl，如果没有则使用默认值
        if (profileInit != null && StringUtils.hasText(profileInit.nickname())) {
            profile.setNickName(profileInit.nickname());
        } else {
            profile.setNickName(buildDefaultNickname(type, identifier, userId));
        }

        if (profileInit != null && StringUtils.hasText(profileInit.avatarUrl())) {
            profile.setAvatarUrl(normalizeAvatarForStorage(profileInit.avatarUrl()));
        } else {
            profile.setAvatarUrl(normalizeAvatarForStorage(null));
        }

        applyDefaultProfileValues(profile);
        userProfileMapper.insert(profile);

        // 5. 导入腾讯IM账号（失败不影响注册流程）
        importTencentIMAccount(userId, profile.getNickName(), profile.getAvatarUrl());

        log.info("新用户注册成功：userId={}, type={}, identifier={}", userId, type, identifier);
        return new LoginUserResult(newUser, true, false, true);
    }

    /**
     * 校验身份未绑定到其他账号
     *
     * @param type 身份类型
     * @param identifier 标识符
     * @param expectedUserId 期望的用户ID（null表示不应存在绑定，非null表示只能绑定到该用户）
     * @throws GloboxApplicationException 如果已绑定到其他账号
     */
    private void assertIdentityNotBoundToOtherUser(AuthIdentity.IdentityType type,
                                                   String identifier,
                                                   Long expectedUserId) {
        LambdaQueryWrapper<AuthIdentity> query = new LambdaQueryWrapper<>();
        query.eq(AuthIdentity::getIdentityType, type)
                .eq(AuthIdentity::getIdentifier, identifier);
        AuthIdentity existingIdentity = authIdentityMapper.selectOne(query);

        if (existingIdentity != null) {
            // 如果expectedUserId为null，表示该身份不应该存在任何绑定
            if (expectedUserId == null) {
                throw new GloboxApplicationException(UserAuthCode.IDENTITY_ALREADY_BOUND);
            }
            // 如果expectedUserId不为null，检查是否绑定到了其他用户
            if (!existingIdentity.getUserId().equals(expectedUserId)) {
                throw new GloboxApplicationException(UserAuthCode.IDENTITY_ALREADY_BOUND);
            }
        }
    }

    /**
     * 根据身份类型构建默认昵称
     */
    private String buildDefaultNickname(AuthIdentity.IdentityType type, String identifier, Long userId) {
        switch (type) {
            case PHONE:
                return buildDefaultNicknameForPhone(identifier);
            case WECHAT:
                return buildDefaultNicknameForWechat(userId);
            case APPLE:
                return "Apple用户" + String.format("%04d", userId % 10000);
            default:
                return "用户" + String.format("%04d", userId % 10000);
        }
    }

    private LoginUserResult ensureUserActiveForLogin(AuthIdentity identity, String defaultNickname) {
        AuthUser authUser = authUserMapper.selectById(identity.getUserId());
        Assert.isNotEmpty(authUser, UserAuthCode.USER_NOT_EXIST);
        if (authUser.getStatus() == AuthUser.UserStatus.DISABLED) {
            throw new GloboxApplicationException(UserAuthCode.USER_ACCOUNT_DISABLED);
        }
        boolean reactivated = false;
        boolean appFirstLogin = false;
        if (Boolean.TRUE.equals(identity.getCancelled()) || Boolean.TRUE.equals(authUser.getCancelled())) {
            reactivateCancelledAccount(authUser.getUserId(), defaultNickname);
            authUser.setCancelled(false);
            authUser.setStatus(AuthUser.UserStatus.ACTIVE);
            reactivated = true;
        } else if (isAppClient() && authUser.getAppFirstLogin()) {
            appFirstLogin = true;
            authUser.setAppFirstLogin(false);
            authUserMapper.updateById(authUser);
        }
        return new LoginUserResult(authUser, false, reactivated, appFirstLogin);
    }

    private void reactivateCancelledAccount(Long userId, String defaultNickname) {
        authUserMapper.update(null, new LambdaUpdateWrapper<AuthUser>()
                .eq(AuthUser::getUserId, userId)
                .set(AuthUser::getCancelled, false)
                .set(AuthUser::getStatus, AuthUser.UserStatus.ACTIVE));
        authIdentityMapper.update(null, new LambdaUpdateWrapper<AuthIdentity>()
                .eq(AuthIdentity::getUserId, userId)
                .set(AuthIdentity::getCancelled, false));
        resetUserProfileForReactivation(userId, defaultNickname);
    }

    private void resetUserProfileForReactivation(Long userId, String defaultNickname) {
        String nickname = StringUtils.hasText(defaultNickname)
                ? defaultNickname
                : "用户" + String.format("%04d", userId % 10000);
        LambdaUpdateWrapper<UserProfile> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserProfile::getUserId, userId)
                .set(UserProfile::getNickName, nickname)
                .set(UserProfile::getAvatarUrl, normalizeAvatarForStorage(null))
                .set(UserProfile::getSignature, DEFAULT_SIGNATURE)
                .set(UserProfile::getGender, GenderEnum.OTHER)
                .set(UserProfile::getSportsStartYear, DEFAULT_SPORTS_YEARS)
                .set(UserProfile::getNtrp, BigDecimal.ZERO)
                .set(UserProfile::getPreferredHand, UserProfile.PreferredHand.RIGHT)
                .set(UserProfile::getHomeDistrict, DEFAULT_HOME_DISTRICT)
                .set(UserProfile::getPower, DEFAULT_POWER)
                .set(UserProfile::getSpeed, DEFAULT_SPEED)
                .set(UserProfile::getServe, DEFAULT_SERVE)
                .set(UserProfile::getVolley, DEFAULT_VOLLEY)
                .set(UserProfile::getStamina, DEFAULT_STAMINA)
                .set(UserProfile::getMental, DEFAULT_MENTAL)
                .set(UserProfile::getCancelled, false);
        // 只在球盒号为空/非法时重新生成，避免每次激活都刷新
        UserProfile current = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .select(UserProfile::getGloboxNo));
        if (current == null || !StringUtils.hasText(current.getGloboxNo())
                || !current.getGloboxNo().matches("^\\d{9}$")) {
            updateWrapper.set(UserProfile::getGloboxNo, generateDefaultGloboxNo())
                    .set(UserProfile::getLastGloboxNoChangedAt, LocalDateTime.now());
        }
        userProfileMapper.update(null, updateWrapper);
        cancelUserProfileRelations(userId);
    }

    private void cancelUserProfileRelations(Long userId) {
        if (userId == null) {
            return;
        }
        LambdaUpdateWrapper<UserRacket> racketUpdate = new LambdaUpdateWrapper<>();
        racketUpdate.eq(UserRacket::getUserId, userId)
                .set(UserRacket::getDeleted, true);
        userRacketMapper.update(null, racketUpdate);

        LambdaUpdateWrapper<UserStyleTag> tagUpdate = new LambdaUpdateWrapper<>();
        tagUpdate.eq(UserStyleTag::getUserId, userId)
                .set(UserStyleTag::getDeleted, true);
        userStyleTagMapper.update(null, tagUpdate);
    }

    private void applyDefaultProfileValues(UserProfile profile) {
        if (profile.getCancelled() == null) {
            profile.setCancelled(false);
        }
        String globoxNo = profile.getGloboxNo();
        if (!StringUtils.hasText(globoxNo) || !globoxNo.matches("^\\d{9}$")) {
            String generated = generateDefaultGloboxNo();
            profile.setGloboxNo(generated);
            profile.setLastGloboxNoChangedAt(LocalDateTime.now());
        }
        if (!StringUtils.hasText(profile.getSignature())) {
            profile.setSignature(DEFAULT_SIGNATURE);
        }
        if (profile.getGender() == null) {
            profile.setGender(GenderEnum.OTHER);
        }
        if (profile.getSportsStartYear() == null) {
            profile.setSportsStartYear(DEFAULT_SPORTS_YEARS);
        }
        if (profile.getNtrp() == null) {
            profile.setNtrp(BigDecimal.ZERO);
        }
        if (profile.getPreferredHand() == null) {
            profile.setPreferredHand(UserProfile.PreferredHand.RIGHT);
        }
        if (profile.getHomeDistrict() == null) {
            profile.setHomeDistrict(DEFAULT_HOME_DISTRICT);
        }
        if (profile.getPower() == null) {
            profile.setPower(DEFAULT_POWER);
        }
        if (profile.getSpeed() == null) {
            profile.setSpeed(DEFAULT_SPEED);
        }
        if (profile.getServe() == null) {
            profile.setServe(DEFAULT_SERVE);
        }
        if (profile.getVolley() == null) {
            profile.setVolley(DEFAULT_VOLLEY);
        }
        if (profile.getStamina() == null) {
            profile.setStamina(DEFAULT_STAMINA);
        }
        if (profile.getMental() == null) {
            profile.setMental(DEFAULT_MENTAL);
        }
    }

    private String generateDefaultGloboxNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyDDD"));
        String key = RedisKeyConstants.GLOBOX_NO_SEQ_PREFIX + datePart;
        long seq = redisService.increment(key, GLOBOX_NO_SEQ_EXPIRE_SECONDS);
        if (seq > GLOBOX_NO_SEQ_MAX) {
            throw new GloboxApplicationException(UserAuthCode.USERNAME_SET_FAILED);
        }
        return datePart + String.format("%04d", seq);
    }

    private String buildDefaultNicknameForPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 4) {
            return null;
        }
        return "用户" + phone.substring(phone.length() - 4);
    }

    private String buildDefaultNicknameForWechat(Long userId) {
        if (userId == null) {
            return null;
        }
        return "微信用户" + String.format("%04d", userId % 10000);
    }

    private boolean isThirdPartyClient() {
        String clientType = RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        return clientType != null && clientType.equalsIgnoreCase(ClientType.THIRD_PARTY_JSAPI.getValue());
    }

    private boolean isAppClient() {
        String clientType = RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        return clientType != null && clientType.equalsIgnoreCase(ClientType.APP.getValue());
    }
    // 仅用于 App 固定登录入口：缺省视为 app，确保 token 有状态
    private String resolveClientTypeForAppLogin(String clientType) {
        if (StringUtils.hasText(clientType)) {
            return clientType;
        }
        return ClientType.APP.getValue();
    }
    // refreshToken 中带 clientType，header 缺失时回填端类型
    private String resolveClientTypeFromRefreshToken(String clientType, String refreshToken) {
        if (StringUtils.hasText(clientType) || !StringUtils.hasText(refreshToken)) {
            return clientType;
        }
        String tokenClientType = JwtUtil.getClaim(refreshToken, jwtSecret, "clientType", String.class);
        if (StringUtils.hasText(tokenClientType)) {
            return tokenClientType;
        }
        return clientType;
    }
    private String generateAccessToken(Long userId,
                                       Map<String, Object> claims,
                                       String secret,
                                       long expireSeconds,
                                       String clientType) {
        Map<String, Object> accessClaims = claims == null ? new HashMap<>() : new HashMap<>(claims);
        if (StringUtils.hasText(clientType) && clientType.equalsIgnoreCase(ClientType.APP.getValue())) {
            String jti = UUID.randomUUID().toString();
            accessClaims.put("jti", jti);
            redisService.saveAccessTokenJti(userId, clientType, jti, expireSeconds);
        }
        return JwtUtil.generateToken(String.valueOf(userId), accessClaims, secret, expireSeconds);
    }
    // refreshToken 写入 clientType，便于刷新时恢复端类型
    private String generateRefreshToken(Long userId,
                                        Map<String, Object> claims,
                                        String secret,
                                        long expireSeconds,
                                        String clientType) {
        Map<String, Object> refreshClaims = claims == null ? new HashMap<>() : new HashMap<>(claims);
        if (StringUtils.hasText(clientType)) {
            refreshClaims.put("clientType", clientType);
        }
        return JwtUtil.generateToken(String.valueOf(userId), refreshClaims, secret, expireSeconds);
    }
    private void saveRefreshTokenForClient(Long userId, String refreshToken, long expireSeconds, String clientType) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        if (StringUtils.hasText(clientType) && clientType.equalsIgnoreCase(ClientType.APP.getValue())) {
            redisService.deleteRefreshTokensByClientType(userId, clientType);
        }
        if (StringUtils.hasText(clientType)) {
            redisService.saveRefreshTokenWithClient(userId, refreshToken, expireSeconds, clientType);
            return;
        }
        redisService.saveRefreshToken(userId, refreshToken, expireSeconds);
    }
    private void assertAppAccessTokenActive(String token, String clientType, String secret) {
        if (!StringUtils.hasText(clientType) || !clientType.equalsIgnoreCase(ClientType.APP.getValue())) {
            return;
        }
        String jti = JwtUtil.getClaim(token, secret, "jti", String.class);
        if (!StringUtils.hasText(jti)) {
            return;
        }
        String userIdStr = JwtUtil.getSubject(token, secret);
        if (!StringUtils.hasText(userIdStr)) {
            throw new GloboxApplicationException(UserAuthCode.TOKEN_INVALID);
        }
        try {
            Long userId = Long.parseLong(userIdStr);
            String cachedJti = redisService.getAccessTokenJti(userId, clientType);
            if (!StringUtils.hasText(cachedJti) || !jti.equals(cachedJti)) {
                throw new GloboxApplicationException(UserAuthCode.TOKEN_INVALID);
            }
        } catch (GloboxApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GloboxApplicationException(UserAuthCode.TOKEN_INVALID);
        }
    }
    private String normalizeAvatarForStorage(String avatarUrl) {
        if (StringUtils.hasText(avatarUrl)) {
            return avatarUrl;
        }
        return "";
    }

    private String resolveAvatarUrlForResponse(String avatarUrl, String clientType) {
        if (StringUtils.hasText(avatarUrl)) {
            return avatarUrl;
        }
        String resolvedClientType = clientType;
        if (!StringUtils.hasText(resolvedClientType)) {
            resolvedClientType = RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        }
        return resolveDefaultAvatarUrl(resolvedClientType);
    }

    private String resolveDefaultAvatarUrl(String clientType) {
        if (StringUtils.hasText(clientType)) {
            ClientType type = ClientType.fromValue(clientType);
            if (ClientType.APP.equals(type) && StringUtils.hasText(userProfileDefaultProperties.getDefaultAvatarUrlApp())) {
                return userProfileDefaultProperties.getDefaultAvatarUrlApp();
            }
            if (ClientType.THIRD_PARTY_JSAPI.equals(type) && StringUtils.hasText(userProfileDefaultProperties.getDefaultAvatarUrlMiniapp())) {
                return userProfileDefaultProperties.getDefaultAvatarUrlMiniapp();
            }
        }
        if (StringUtils.hasText(userProfileDefaultProperties.getDefaultAvatarUrl())) {
            return userProfileDefaultProperties.getDefaultAvatarUrl();
        }
        return "";
    }

    /**
     * Apple登录
     *
     * @param request Apple登录请求
     * @return 登录响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<LoginResponse> appleLogin(AppleLoginRequest request) {
        String identityToken = request.getIdentityToken();
        String clientType = resolveClientTypeForAppLogin(
                RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE));
        // 1. 验证 identityToken 不为空
        Assert.isNotEmpty(identityToken, UserAuthCode.INVALID_PARAM);

        // 2. 验证 identityToken 并提取 sub（Apple 用户唯一标识）
        String appleSub = appleService.verifyAndExtractSub(identityToken);
        Assert.isNotEmpty(appleSub, UserAuthCode.INVALID_PARAM);

        // 3. 调用统一登录方法
        LoginUserResult loginResult = loginOrRegisterByIdentity(
                AuthIdentity.IdentityType.APPLE,
                appleSub,
                new ProfileInit("Apple用户" + appleSub.substring(Math.max(0, appleSub.length() - 4)), null)
        );
        AuthUser authUser = loginResult.authUser();
        boolean created = loginResult.created();
        boolean reactivated = loginResult.reactivated();
        boolean appFirstLogin = loginResult.appFirstLogin();
        boolean isNewUser = created || reactivated || appFirstLogin;

        // 4. 生成 JWT Token（复用 phoneLogin 的逻辑）
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", authUser.getRole());
        String accessToken = generateAccessToken(
                authUser.getUserId(),
                claims,
                jwtSecret,
                accessTokenExpire,
                clientType
        );

        String refreshToken = generateRefreshToken(
                authUser.getUserId(),
                claims,
                jwtSecret,
                refreshTokenExpire,
                clientType
        );
        // 5. 保存 Refresh Token 到 Redis
        saveRefreshTokenForClient(authUser.getUserId(), refreshToken, refreshTokenExpire, clientType);
        // 6. 记录登录日志
        recordLoginLog(authUser.getUserId(), appleSub, AuthIdentity.IdentityType.APPLE, true, null);

        // 7. 注册设备（如果提供了设备信息）
        registerDeviceIfPresent(authUser.getUserId(), authUser.getRole().name(), request.getDeviceInfo());

        // 8. 构建响应
        LoginResponse response = LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(authUser.getUserId())
                .roles(Collections.singletonList(authUser.getRole().name()))
                .isNewUser(isNewUser)
                .reactivated(reactivated)
                .build();

        log.info("Apple登录成功：userId={}, appleSub={}", authUser.getUserId(), appleSub);
        return R.ok(response);
    }
}
