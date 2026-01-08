package com.unlimited.sports.globox.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.model.auth.entity.InternalTestWhitelist;
import com.unlimited.sports.globox.user.mapper.WhitelistMapper;
import com.unlimited.sports.globox.user.service.WhitelistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 白名单服务实现
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Service
@Slf4j
public class WhitelistServiceImpl implements WhitelistService {

    @Autowired
    private WhitelistMapper whitelistMapper;

    @Value("${auth.phone-whitelist.enabled:true}")
    private boolean phoneWhitelistEnabled;

    /**
     * 白名单校验（临时功能，计划移除日期：2026/01/31）
     * 说明：当前仅限内测用户使用，正式上线后移除
     */
    @Override
    public boolean isInWhitelist(String phone) {
        // 如果白名单功能已关闭，直接放行
        if (!phoneWhitelistEnabled) {
            log.info("手机号白名单已关闭，直接放行：{}", phone);
            return true;
        }

        // 白名单功能开启，执行原有校验逻辑
        LambdaQueryWrapper<InternalTestWhitelist> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InternalTestWhitelist::getPhone, phone)
                    .eq(InternalTestWhitelist::getStatus, InternalTestWhitelist.WhitelistStatus.ACTIVE);

        InternalTestWhitelist whitelist = whitelistMapper.selectOne(queryWrapper);
        boolean inWhitelist = whitelist != null;

        if (inWhitelist) {
            log.info("手机号 {} 在白名单内，邀请来源：{}", phone, whitelist.getInviteSource());
        } else {
            log.warn("手机号 {} 不在白名单内，拒绝操作", phone);
        }

        return inWhitelist;
    }

    @Override
    public InternalTestWhitelist getWhitelistByPhone(String phone) {
        LambdaQueryWrapper<InternalTestWhitelist> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InternalTestWhitelist::getPhone, phone);
        return whitelistMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean addToWhitelist(String phone, String inviteSource) {
        InternalTestWhitelist existing = getWhitelistByPhone(phone);
        if (existing != null) {
            log.warn("手机号 {} 已在白名单中", phone);
            return false;
        }

        InternalTestWhitelist whitelist = new InternalTestWhitelist();
        whitelist.setPhone(phone);
        whitelist.setInviteSource(inviteSource);
        whitelist.setStatus(InternalTestWhitelist.WhitelistStatus.ACTIVE);

        int result = whitelistMapper.insert(whitelist);

        if (result > 0) {
            log.info("手机号 {} 已添加到白名单，邀请来源：{}", phone, inviteSource);
            return true;
        }

        return false;
    }
}
