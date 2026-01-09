package com.unlimited.sports.globox.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.utils.AuthContextHolder;
import com.unlimited.sports.globox.common.utils.HttpRequestUtils;
import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.model.auth.dto.ChangePasswordRequest;
import com.unlimited.sports.globox.model.auth.dto.DeviceInfo;
import com.unlimited.sports.globox.model.auth.dto.DeviceRegisterRequest;
import com.unlimited.sports.globox.model.auth.dto.LoginResponse;
import com.unlimited.sports.globox.model.auth.dto.PasswordLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.PhoneLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.ResetPasswordRequest;
import com.unlimited.sports.globox.model.auth.dto.SendCaptchaRequest;
import com.unlimited.sports.globox.model.auth.dto.SetPasswordRequest;
import com.unlimited.sports.globox.model.auth.dto.TokenRefreshRequest;
import com.unlimited.sports.globox.model.auth.dto.ThirdPartyLoginResponse;
import com.unlimited.sports.globox.model.auth.dto.WechatBindPhoneRequest;
import com.unlimited.sports.globox.model.auth.dto.WechatLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.WechatLoginResponse;
import com.unlimited.sports.globox.model.auth.dto.WechatPhoneLoginRequest;
import com.unlimited.sports.globox.model.auth.vo.WechatUserInfo;
import com.unlimited.sports.globox.model.auth.entity.AuthIdentity;
import com.unlimited.sports.globox.model.auth.entity.AuthUser;
import com.unlimited.sports.globox.model.auth.entity.UserLoginRecord;
import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import com.unlimited.sports.globox.user.mapper.AuthIdentityMapper;
import com.unlimited.sports.globox.user.mapper.AuthUserMapper;
import com.unlimited.sports.globox.user.mapper.UserLoginRecordMapper;
import com.unlimited.sports.globox.user.mapper.UserProfileMapper;
import com.unlimited.sports.globox.user.service.AuthService;
import com.unlimited.sports.globox.user.service.IUserDeviceService;
import com.unlimited.sports.globox.user.service.RedisService;
import com.unlimited.sports.globox.user.service.SmsService;
import com.unlimited.sports.globox.user.service.WechatService;
import com.unlimited.sports.globox.user.service.WhitelistService;
import com.unlimited.sports.globox.common.utils.JwtUtil;
import com.unlimited.sports.globox.user.util.PasswordUtils;
import com.unlimited.sports.globox.user.util.PhoneUtils;
import com.unlimited.sports.globox.dubbo.social.ChatDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private WechatService wechatService;

    @Autowired
    private IUserDeviceService userDeviceService;

    @DubboReference(group = "rpc")
    private ChatDubboService chatDubboService;

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
    private static final int MAX_SMS_ERROR_COUNT = 5; // 最大错误次数

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

        log.info("jwtSecret={}", jwtSecret);

        // 1. 验证手机号格式
        Assert.isTrue(PhoneUtils.isValidPhone(phone), UserAuthCode.INVALID_PHONE);

        // 2. 白名单校验
        Assert.isTrue(whitelistService.isInWhitelist(phone), UserAuthCode.NOT_IN_WHITELIST);

        // 3. 验证错误次数检查
        long errorCount = redisService.getSmsErrorCount(phone);
        Assert.isTrue(errorCount < MAX_SMS_ERROR_COUNT, UserAuthCode.CAPTCHA_ERROR_TOO_MANY);

        // 4. 验证码校验
        String savedCode = redisService.getSmsCode(phone);
        Assert.isNotEmpty(savedCode, UserAuthCode.INVALID_CAPTCHA);

        if (!code.equals(savedCode)) {
            // 错误次数+1
            redisService.incrementSmsError(phone);
            // 记录登录失败日志
            recordLoginLog(null, phone, AuthIdentity.IdentityType.PHONE, false, UserAuthCode.INVALID_CAPTCHA.getMessage());
            throw new GloboxApplicationException(UserAuthCode.INVALID_CAPTCHA);
        }

        // 5. 验证码正确，清除错误计数和验证码
        redisService.clearSmsError(phone);
        redisService.deleteSmsCode(phone);

        // 6. 查询或创建用户（登录即注册）
        boolean[] isNewUserFlag = new boolean[1]; // 使用数组传递引用
        AuthUser authUser = getOrCreateUser(phone, isNewUserFlag);
        boolean isNewUser = isNewUserFlag[0];

        // 7. 生成JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", authUser.getRole());

        String accessToken = JwtUtil.generateToken(
                String.valueOf(authUser.getUserId()),
                claims,
                jwtSecret,
                accessTokenExpire
        );

        String refreshToken = JwtUtil.generateToken(
                String.valueOf(authUser.getUserId()),
                claims,
                jwtSecret,
                refreshTokenExpire
        );

        // 8. 保存Refresh Token到Redis
        redisService.saveRefreshToken(authUser.getUserId(), refreshToken, refreshTokenExpire);

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
                .build();

        log.info("用户登录成功：userId={}, phone={}", authUser.getUserId(), phone);
        return R.ok(response);
    }

    /**
     * 查询或创建用户（登录即注册）
     *
     * @param phone 手机号
     * @param isNewUserFlag 是否为新用户的标识（输出参数，true表示新创建的用户）
     * @return 用户信息
     */
    private AuthUser getOrCreateUser(String phone, boolean[] isNewUserFlag) {
        // 1. 查询auth_identity，看该手机号是否已注册
        LambdaQueryWrapper<AuthIdentity> identityQuery = new LambdaQueryWrapper<>();
        identityQuery.eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE)
                .eq(AuthIdentity::getIdentifier, phone);
        AuthIdentity identity = authIdentityMapper.selectOne(identityQuery);

        if (identity != null) {
            // 已注册，根据userId查询用户信息
            isNewUserFlag[0] = false;
            LambdaQueryWrapper<AuthUser> userQuery = new LambdaQueryWrapper<>();
            userQuery.eq(AuthUser::getUserId, identity.getUserId());
            AuthUser authUser = authUserMapper.selectOne(userQuery);
            Assert.isNotEmpty(authUser, UserAuthCode.USER_NOT_EXIST);
            return authUser;
        }

        // 2. 未注册，创建新用户（时间字段由数据库自动填充）
        isNewUserFlag[0] = true;
        AuthUser newUser = new AuthUser();
        // userId由数据库自动生成（自增主键）
        newUser.setRole(AuthUser.UserRole.USER);
        newUser.setStatus(AuthUser.UserStatus.ACTIVE);

        int insertResult = authUserMapper.insert(newUser);
        Assert.isTrue(insertResult > 0, UserAuthCode.USER_NOT_EXIST);
        // 插入后获取自增的userId
        Long userId = newUser.getUserId();
        Assert.isNotEmpty(userId, UserAuthCode.USER_NOT_EXIST);

        // 3. 创建身份记录（手机号，时间字段由数据库自动填充）
        AuthIdentity newIdentity = new AuthIdentity();
        // 生成业务主键identityId（UUID）
        newIdentity.setIdentityId(UUID.randomUUID().toString());
        newIdentity.setUserId(userId);
        newIdentity.setIdentityType(AuthIdentity.IdentityType.PHONE);
        newIdentity.setIdentifier(phone);
        newIdentity.setCredential(null); // 验证码登录无密码
        newIdentity.setVerified(true);

        authIdentityMapper.insert(newIdentity);

        // 4. 创建用户资料（时间字段由数据库自动填充）
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setNickName("用户" + phone.substring(phone.length() - 4)); // 默认昵称：用户+后4位

        userProfileMapper.insert(profile);

        // 导入腾讯IM账号（失败不影响注册流程）
        importTencentIMAccount(userId, profile.getNickName(), profile.getAvatarUrl());

        log.info("新用户注册成功：userId={}, phone={}", userId, phone);
        return newUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<WechatLoginResponse> wechatLogin(WechatLoginRequest request) {
        String code = request.getCode();

        // 1. 获取客户端类型
        String clientType = AuthContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        if (!StringUtils.hasText(clientType)) {
            log.error("【微信登录】缺少X-Client-Type请求头");
            throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
        }

        // 2. 调用微信API换取openid/unionid
        WechatUserInfo wechatUserInfo = wechatService.getOpenIdAndUnionId(code, clientType);
        String openid = wechatUserInfo.getOpenid();
        String unionid = wechatUserInfo.getUnionid();
        // DEV ONLY: log openid for debugging, remove before release.
        log.info("DEV ONLY wechat openid={}", openid);

        // 2. 优先使用unionid，如果没有则使用openid
        String identifier = unionid != null ? unionid : openid;

        // 3. 查询auth_identity表，判断是否已绑定
        LambdaQueryWrapper<AuthIdentity> identityQuery = new LambdaQueryWrapper<>();
        identityQuery.eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.WECHAT)
                .eq(AuthIdentity::getIdentifier, identifier);
        AuthIdentity identity = authIdentityMapper.selectOne(identityQuery);

        if (identity != null) {
            // 4. 已绑定：直接登录
            LambdaQueryWrapper<AuthUser> userQuery = new LambdaQueryWrapper<>();
            userQuery.eq(AuthUser::getUserId, identity.getUserId());
            AuthUser authUser = authUserMapper.selectOne(userQuery);
            Assert.isNotEmpty(authUser, UserAuthCode.USER_NOT_EXIST);

            boolean isThirdParty = isThirdPartyClient();

            // 生成JWT Token
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", authUser.getRole());

            String accessToken;
            String refreshToken = null;
            if (isThirdParty) {
                claims.put("clientType", ClientType.THIRD_PARTY_JSAPI.getValue());
                claims.put("openid", openid);
                if (!StringUtils.hasText(thirdPartyJwtSecret)) {
                    log.error("第三方小程序 JWT secret 未配置");
                    throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
                }
                accessToken = JwtUtil.generateToken(
                        String.valueOf(authUser.getUserId()),
                        claims,
                        thirdPartyJwtSecret,
                        thirdPartyAccessTokenExpire
                );
            } else {
                accessToken = JwtUtil.generateToken(
                        String.valueOf(authUser.getUserId()),
                        claims,
                        jwtSecret,
                        accessTokenExpire
                );

                refreshToken = JwtUtil.generateToken(
                        String.valueOf(authUser.getUserId()),
                        claims,
                        jwtSecret,
                        refreshTokenExpire
                );
                // 保存Refresh Token到Redis
                redisService.saveRefreshToken(authUser.getUserId(), refreshToken, refreshTokenExpire);
            }

            // 记录登录日志
            recordLoginLog(authUser.getUserId(), openid, AuthIdentity.IdentityType.WECHAT, true, null);

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
                    .avatarUrl(userProfile != null ? userProfile.getAvatarUrl() : null)
                    .build();

            // 已绑定直接登录，认为不是新用户
            boolean isNewUser = false;

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
            // 5. 未绑定：生成临时凭证
            String tempToken = UUID.randomUUID().toString();
            redisService.saveWechatTempToken(tempToken, identifier, 5); // TTL=5分钟

            WechatLoginResponse response = WechatLoginResponse.builder()
                    .needBindPhone(true)
                    .tempToken(tempToken)
                    .build();

            log.info("微信未绑定，返回临时凭证：openid={}", openid);
            return R.ok(response);
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

        // 1. 验证临时凭证
        String identifier = redisService.getWechatTempToken(tempToken);
        Assert.isNotEmpty(identifier, UserAuthCode.TEMP_TOKEN_EXPIRED);

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

        // 7. 查询手机号是否已注册
        LambdaQueryWrapper<AuthIdentity> phoneIdentityQuery = new LambdaQueryWrapper<>();
        phoneIdentityQuery.eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE)
                .eq(AuthIdentity::getIdentifier, phone);
        AuthIdentity phoneIdentity = authIdentityMapper.selectOne(phoneIdentityQuery);

        Long userId;
        boolean isNewUser = false;

        if (phoneIdentity != null) {
            // 7a. 已注册：绑定微信到现有账号
            userId = phoneIdentity.getUserId();

            // 检查该微信是否已绑定其他账号
            LambdaQueryWrapper<AuthIdentity> wechatCheckQuery = new LambdaQueryWrapper<>();
            wechatCheckQuery.eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.WECHAT)
                    .eq(AuthIdentity::getIdentifier, identifier);
            AuthIdentity existingWechatIdentity = authIdentityMapper.selectOne(wechatCheckQuery);
            Assert.isTrue(existingWechatIdentity == null || existingWechatIdentity.getUserId().equals(userId),
                    UserAuthCode.WECHAT_AUTH_FAILED);

            // 如果微信未绑定，创建绑定记录
            if (existingWechatIdentity == null) {
                AuthIdentity wechatIdentity = new AuthIdentity();
                wechatIdentity.setIdentityId(UUID.randomUUID().toString());
                wechatIdentity.setUserId(userId);
                wechatIdentity.setIdentityType(AuthIdentity.IdentityType.WECHAT);
                wechatIdentity.setIdentifier(identifier);
                wechatIdentity.setCredential(null);
                wechatIdentity.setVerified(true);
                authIdentityMapper.insert(wechatIdentity);
            }

        log.info("微信绑定到现有账号：userId={}, phone={}", userId, phone);
        } else {
            // 7b. 未注册：创建新账号
            isNewUser = true;

            // 创建auth_user
            AuthUser newUser = new AuthUser();
            // userId由数据库自动生成（自增主键）
            newUser.setRole(AuthUser.UserRole.USER);
            newUser.setStatus(AuthUser.UserStatus.ACTIVE);
            int insertResult = authUserMapper.insert(newUser);
            Assert.isTrue(insertResult > 0, UserAuthCode.USER_NOT_EXIST);
            // 插入后获取自增的userId
            userId = newUser.getUserId();
            Assert.isNotEmpty(userId, UserAuthCode.USER_NOT_EXIST);

            // 创建auth_identity（PHONE类型）
            AuthIdentity phoneIdentityNew = new AuthIdentity();
            phoneIdentityNew.setIdentityId(UUID.randomUUID().toString());
            phoneIdentityNew.setUserId(userId);
            phoneIdentityNew.setIdentityType(AuthIdentity.IdentityType.PHONE);
            phoneIdentityNew.setIdentifier(phone);
            phoneIdentityNew.setCredential(null);
            phoneIdentityNew.setVerified(true);
            authIdentityMapper.insert(phoneIdentityNew);

            // 创建auth_identity（WECHAT类型）
            AuthIdentity wechatIdentity = new AuthIdentity();
            wechatIdentity.setIdentityId(UUID.randomUUID().toString());
            wechatIdentity.setUserId(userId);
            wechatIdentity.setIdentityType(AuthIdentity.IdentityType.WECHAT);
            wechatIdentity.setIdentifier(identifier);
            wechatIdentity.setCredential(null);
            wechatIdentity.setVerified(true);
            authIdentityMapper.insert(wechatIdentity);

            // 创建user_profile，自动填充微信信息
            UserProfile profile = new UserProfile();
            profile.setUserId(userId);
            // 如果提供了微信昵称，使用微信昵称，否则使用默认昵称
            if (nickname != null && !nickname.trim().isEmpty()) {
                profile.setNickName(nickname);
            } else {
                profile.setNickName("用户" + phone.substring(phone.length() - 4));
            }
            // 如果提供了微信头像，使用微信头像
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                profile.setAvatarUrl(avatarUrl);
            }
            userProfileMapper.insert(profile);

            // 导入腾讯IM账号（失败不影响注册流程）
            importTencentIMAccount(userId, profile.getNickName(), profile.getAvatarUrl());

        log.info("新用户注册成功（微信登录）：userId={}, phone={}", userId, phone);
        }

        // 8. 删除临时凭证
        redisService.deleteWechatTempToken(tempToken);

        // 9. 生成JWT双Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", AuthUser.UserRole.USER);

        String accessToken = JwtUtil.generateToken(
                String.valueOf(userId),
                claims,
                jwtSecret,
                accessTokenExpire
        );

        String refreshToken = JwtUtil.generateToken(
                String.valueOf(userId),
                claims,
                jwtSecret,
                refreshTokenExpire
        );

        // 10. 保存Refresh Token到Redis
        redisService.saveRefreshToken(userId, refreshToken, refreshTokenExpire);

        // 11. 记录登录日志
        recordLoginLog(userId, phone, AuthIdentity.IdentityType.WECHAT, true, null);

        // 12. 注册设备（如果提供了设备信息）
        registerDeviceIfPresent(userId, AuthUser.UserRole.USER.name(), request.getDeviceInfo());

        // 13. 构建响应（token 为纯 JWT 字符串）
        LoginResponse response = LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .roles(Collections.singletonList(AuthUser.UserRole.USER.name()))
                .isNewUser(isNewUser)
                .build();

        log.info("微信绑定手机号成功：userId={}, phone={}", userId, phone);
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
        String clientType = AuthContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        if (!StringUtils.hasText(clientType)) {
            log.error("【微信手机号登录】缺少X-Client-Type请求头");
            throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
        }

        // 2. 使用wxCode获取openid/unionid（避免重复使用wxCode）
        WechatUserInfo wechatUserInfo = wechatService.getOpenIdAndUnionId(wxCode, clientType);
        String openid = wechatUserInfo.getOpenid();
        String unionid = wechatUserInfo.getUnionid();
        // TODO: DEV ONLY: 开发环境打印openid,正式上线请移除
        log.info("DEV ONLY wechat openid={}", openid);
        // 优先使用unionid，如果没有则使用openid
        String identifier = unionid != null ? unionid : openid;

        // 3. 使用phoneCode获取手机号（不再重复消耗wxCode）
        String phone = wechatService.getPhoneNumber(wxCode, phoneCode, clientType);

        // 4. 验证手机号格式
        Assert.isTrue(PhoneUtils.isValidPhone(phone), UserAuthCode.INVALID_PHONE);

        // 5. 白名单校验
        Assert.isTrue(whitelistService.isInWhitelist(phone), UserAuthCode.NOT_IN_WHITELIST);

        // 6. 查询手机号是否已注册
        LambdaQueryWrapper<AuthIdentity> phoneIdentityQuery = new LambdaQueryWrapper<>();
        phoneIdentityQuery.eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE)
                .eq(AuthIdentity::getIdentifier, phone);
        AuthIdentity phoneIdentity = authIdentityMapper.selectOne(phoneIdentityQuery);

        Long userId;
        boolean isNewUser = false;

        if (phoneIdentity != null) {
            // 5a. 已注册：绑定微信到现有账号（如果未绑定）
            userId = phoneIdentity.getUserId();

            // 检查该微信是否已绑定其他账号
            LambdaQueryWrapper<AuthIdentity> wechatCheckQuery = new LambdaQueryWrapper<>();
            wechatCheckQuery.eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.WECHAT)
                    .eq(AuthIdentity::getIdentifier, identifier);
            AuthIdentity existingWechatIdentity = authIdentityMapper.selectOne(wechatCheckQuery);
            Assert.isTrue(existingWechatIdentity == null || existingWechatIdentity.getUserId().equals(userId),
                    UserAuthCode.WECHAT_AUTH_FAILED);

            // 如果微信未绑定，创建绑定记录
            if (existingWechatIdentity == null) {
                AuthIdentity wechatIdentity = new AuthIdentity();
                wechatIdentity.setIdentityId(UUID.randomUUID().toString());
                wechatIdentity.setUserId(userId);
                wechatIdentity.setIdentityType(AuthIdentity.IdentityType.WECHAT);
                wechatIdentity.setIdentifier(identifier);
                wechatIdentity.setCredential(null);
                wechatIdentity.setVerified(true);
                authIdentityMapper.insert(wechatIdentity);
            }

            log.info("第三方小程序微信登录：绑定到现有账号：userId={}, phone={}", userId, phone);
        } else {
            // 5b. 未注册：创建新账号
            isNewUser = true;

            // 创建auth_user
            AuthUser newUser = new AuthUser();
            newUser.setRole(AuthUser.UserRole.USER);
            newUser.setStatus(AuthUser.UserStatus.ACTIVE);
            int insertResult = authUserMapper.insert(newUser);
            Assert.isTrue(insertResult > 0, UserAuthCode.USER_NOT_EXIST);
            userId = newUser.getUserId();
            Assert.isNotEmpty(userId, UserAuthCode.USER_NOT_EXIST);

            // 创建auth_identity（PHONE类型）
            AuthIdentity phoneIdentityNew = new AuthIdentity();
            phoneIdentityNew.setIdentityId(UUID.randomUUID().toString());
            phoneIdentityNew.setUserId(userId);
            phoneIdentityNew.setIdentityType(AuthIdentity.IdentityType.PHONE);
            phoneIdentityNew.setIdentifier(phone);
            phoneIdentityNew.setCredential(null);
            phoneIdentityNew.setVerified(true);
            authIdentityMapper.insert(phoneIdentityNew);

            // 创建auth_identity（WECHAT类型）
            AuthIdentity wechatIdentity = new AuthIdentity();
            wechatIdentity.setIdentityId(UUID.randomUUID().toString());
            wechatIdentity.setUserId(userId);
            wechatIdentity.setIdentityType(AuthIdentity.IdentityType.WECHAT);
            wechatIdentity.setIdentifier(identifier);
            wechatIdentity.setCredential(null);
            wechatIdentity.setVerified(true);
            authIdentityMapper.insert(wechatIdentity);

            // 创建user_profile，使用提供的nickname和avatarUrl
            UserProfile profile = new UserProfile();
            profile.setUserId(userId);
            if (nickname != null && !nickname.trim().isEmpty()) {
                profile.setNickName(nickname);
            } else {
                profile.setNickName("用户" + phone.substring(phone.length() - 4));
            }
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                profile.setAvatarUrl(avatarUrl);
            }
            userProfileMapper.insert(profile);

            // 导入腾讯IM账号（失败不影响注册流程）
            importTencentIMAccount(userId, profile.getNickName(), profile.getAvatarUrl());

            log.info("第三方小程序新用户注册成功：userId={}, phone={}", userId, phone);
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
            claims.put("openid", openid);
            if (!StringUtils.hasText(thirdPartyJwtSecret)) {
                log.error("第三方小程序 JWT secret 未配置");
                throw new GloboxApplicationException(UserAuthCode.WECHAT_AUTH_FAILED);
            }
            accessToken = JwtUtil.generateToken(
                    String.valueOf(userId),
                    claims,
                    thirdPartyJwtSecret,
                    thirdPartyAccessTokenExpire
            );
            // 第三方小程序不生成 refreshToken
        } else {
            // App端：使用 user.jwt.* 配置
            accessToken = JwtUtil.generateToken(
                    String.valueOf(userId),
                    claims,
                    jwtSecret,
                    accessTokenExpire
            );
            refreshToken = JwtUtil.generateToken(
                    String.valueOf(userId),
                    claims,
                    jwtSecret,
                    refreshTokenExpire
            );
            // 保存Refresh Token到Redis
            redisService.saveRefreshToken(userId, refreshToken, refreshTokenExpire);
        }

        // 9. 记录登录日志
        recordLoginLog(userId, phone, AuthIdentity.IdentityType.WECHAT, true, null);

        // 10. 构建响应
        ThirdPartyLoginResponse.UserInfo userInfo = ThirdPartyLoginResponse.UserInfo.builder()
                .id(userId)
                .phone(phone)
                .nickname(userProfile != null ? userProfile.getNickName() : null)
                .avatarUrl(userProfile != null ? userProfile.getAvatarUrl() : null)
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
        String authHeader = AuthContextHolder.getHeader("Authorization");
        String token = JwtUtil.extractTokenFromHeader(authHeader);
        Assert.isNotEmpty(token, UserAuthCode.TOKEN_INVALID);

        // 2. 验证Token有效性
        Assert.isTrue(JwtUtil.validateToken(token, jwtSecret), UserAuthCode.TOKEN_INVALID);

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

        // 8. 查询用户信息
        LambdaQueryWrapper<AuthUser> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(AuthUser::getUserId, identity.getUserId());
        AuthUser authUser = authUserMapper.selectOne(userQuery);
        Assert.isNotEmpty(authUser, UserAuthCode.USER_NOT_EXIST);

        // 9. 生成JWT双Token（完全复用phoneLogin的逻辑）
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", authUser.getRole());

        String accessToken = JwtUtil.generateToken(
                String.valueOf(authUser.getUserId()),
                claims,
                jwtSecret,
                accessTokenExpire
        );

        String refreshToken = JwtUtil.generateToken(
                String.valueOf(authUser.getUserId()),
                claims,
                jwtSecret,
                refreshTokenExpire
        );

        // 10. 保存Refresh Token到Redis
        redisService.saveRefreshToken(authUser.getUserId(), refreshToken, refreshTokenExpire);

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
    public R<String> changePassword(ChangePasswordRequest request) {
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        // 1. 从请求头提取Token
        String authHeader = AuthContextHolder.getHeader("Authorization");
        String token = JwtUtil.extractTokenFromHeader(authHeader);
        Assert.isNotEmpty(token, UserAuthCode.TOKEN_INVALID);

        // 2. 验证Token有效性
        Assert.isTrue(JwtUtil.validateToken(token, jwtSecret), UserAuthCode.TOKEN_INVALID);

        // 3. 从Token中提取userId
        Long userId = Long.parseLong(JwtUtil.getSubject(token, jwtSecret));

        // 4. 查询用户身份
        LambdaQueryWrapper<AuthIdentity> identityQuery = new LambdaQueryWrapper<>();
        identityQuery.eq(AuthIdentity::getUserId, userId)
                .eq(AuthIdentity::getIdentityType, AuthIdentity.IdentityType.PHONE);
        AuthIdentity identity = authIdentityMapper.selectOne(identityQuery);
        Assert.isNotEmpty(identity, UserAuthCode.USER_NOT_EXIST);

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

        // 1. 校验 JWT 签名和过期时间
        if (!JwtUtil.validateToken(refreshToken, jwtSecret)) {
            throw new GloboxApplicationException(UserAuthCode.REFRESH_TOKEN_INVALID);
        }

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

        // 5. 删除旧 refreshToken（token 旋转：旧 token 立即失效）
        redisService.deleteRefreshToken(refreshToken, jwtSecret);

        // 6. 生成新的 access token 和 refresh token
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", authUser.getRole());

        String newAccessToken = JwtUtil.generateToken(
                String.valueOf(userId),
                claims,
                jwtSecret,
                accessTokenExpire
        );

        String newRefreshToken = JwtUtil.generateToken(
                String.valueOf(userId),
                claims,
                jwtSecret,
                refreshTokenExpire
        );

        // 7. 保存新 refresh token 到 Redis
        redisService.saveRefreshToken(userId, newRefreshToken, refreshTokenExpire);

        // 8. 构建响应
        LoginResponse response = LoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(userId)
                .roles(Collections.singletonList(authUser.getRole().name()))
                .isNewUser(false)
                .build();

        log.info("Token刷新成功：userId={}", userId);
        return R.ok(response);
    }

    /**
     * 记录登录日志（时间字段由数据库自动填充）
     *
     * @param userId 用户ID（失败时可能为null）
     * @param identifier 标识（手机号或openid等，会自动脱敏）
     * @param identityType 登录方式类型
     * @param success 是否成功
     * @param failReason 失败原因
     */
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
            
            Boolean result = chatDubboService.accountImport(userIdStr, userName, faceUrl);
            
            if (Boolean.TRUE.equals(result)) {
                log.info("IM账号导入成功: userId={}, nickName={}", userId, userName);
            } else {
                log.warn("IM账号导入失败: userId={}, nickName={}, result={}", userId, userName, result);
            }
        } catch (Exception e) {
            log.error("IM账号导入异常: userId={}, nickName={}, error={}", userId, userName, e.getMessage(), e);
            // 异常不影响注册登录流程，仅记录日志
        }
    }

    private boolean isThirdPartyClient() {
        String clientType = AuthContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);
        return clientType != null && clientType.equalsIgnoreCase(ClientType.THIRD_PARTY_JSAPI.getValue());
    }
}
