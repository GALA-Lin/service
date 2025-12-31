package com.unlimited.sports.globox.notification.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.notification.dto.DeviceRegisterRequest;
import com.unlimited.sports.globox.model.notification.entity.UserDevices;
import com.unlimited.sports.globox.notification.service.IUserDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 用户设备管理
 */
@Slf4j
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
@Validated
public class UserDeviceController {

    @Autowired
    private  IUserDeviceService userDeviceService;

    /**
     * 注册设备
     */
    @PostMapping("/register")
    public R<UserDevices> registerDevice(@Valid @RequestBody DeviceRegisterRequest request,
    HttpServletRequest httpRequest) {
        String userIdStr = httpRequest.getHeader(RequestHeaderConstants.HEADER_USER_ID);
        if(StringUtils.isBlank(userIdStr)) {
            throw new GloboxApplicationException("没有获取到userId");
        }
        request.setUserId(Long.valueOf(userIdStr));
        log.info("[设备管理] 注册设备: userId={}, deviceId={}", request.getUserId(), request.getDeviceId());
        UserDevices device = userDeviceService.registerDevice(request);
        return R.ok(device);
    }

    /**
     * 注销设备
     */
    @PostMapping("/logout")
    public R<Boolean> logoutDevice(@RequestParam String deviceId,HttpServletRequest httpRequest) {
        String userIdStr = httpRequest.getHeader(RequestHeaderConstants.HEADER_USER_ID);
        if(StringUtils.isBlank(userIdStr)) {
            throw new GloboxApplicationException("没有获取到userId");
        }

        boolean success = userDeviceService.logoutDevice(Long.valueOf(userIdStr), deviceId);
        return R.ok(success);
    }


    /**
     * 启用推送
     */
    @PostMapping("/push/enable")
    public R<Boolean> enablePush(HttpServletRequest httpRequest) {
        String userIdStr = httpRequest.getHeader(RequestHeaderConstants.HEADER_USER_ID);
        if(StringUtils.isBlank(userIdStr)) {
            throw new GloboxApplicationException("没有获取到userId");
        }

        boolean success = userDeviceService.enablePush(Long.valueOf(userIdStr));
        return R.ok(success);
    }

    /**
     * 禁用推送
     */
    @PostMapping("/push/disable")
    public R<Boolean> disablePush(HttpServletRequest httpRequest) {
        String userIdStr = httpRequest.getHeader(RequestHeaderConstants.HEADER_USER_ID);
        if(StringUtils.isBlank(userIdStr)) {
            throw new GloboxApplicationException("没有获取到userId");
        }
        boolean success = userDeviceService.disablePush(Long.valueOf(userIdStr));
        return R.ok(success);
    }
}
