package com.unlimited.sports.globox.social.handler;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 社交模块异常处理器
 * 处理缺少请求头等异常情况
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SocialModuleExceptionHandler {

    /**
     * 处理缺少请求头异常
     * 当需要认证的接口缺少 userId 请求头时触发
     * 通常是因为请求未通过网关认证或直接访问了需要认证的接口
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public R<Void> handleMissingRequestHeader(MissingRequestHeaderException ex) {
        log.error("缺少请求头: {}", ex.getMessage(), ex);
        // 如果缺少的是 userId 请求头，返回需要认证的错误
        if (ex.getHeaderName() != null && ex.getHeaderName().contains("User-Id")) {
            return R.<Void>error(UserAuthCode.TOKEN_INVALID)
                    .message("需要登录后才能访问此接口");
        }
        return R.<Void>error(UserAuthCode.MISSING_USER_ID_HEADER)
                .message("缺少请求头：" + ex.getHeaderName());
    }
}

