package com.unlimited.sports.globox.user.handler;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.validation.ConstraintViolationException;

/**
 * 用户模块异常处理
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserModuleExceptionHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    public R<Void> handleMissingRequestHeader(MissingRequestHeaderException ex) {
        log.error("An error occurred: {}", ex.getMessage(), ex);
        return R.<Void>error(UserAuthCode.MISSING_USER_ID_HEADER)
                .message("缺少请求头：" + ex.getHeaderName());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public R<Void> handleMissingRequestPart(MissingServletRequestPartException ex) {
        log.error("An error occurred: {}", ex.getMessage(), ex);
        return R.<Void>error(UserAuthCode.MISSING_UPLOAD_FILE)
                .message("缺少上传文件：" + ex.getRequestPartName());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.error("An error occurred: {}", ex.getMessage(), ex);
        return R.error(UserAuthCode.UPLOAD_FILE_TOO_LARGE);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public R<Void> handleValidationException(Exception ex) {
        log.error("An error occurred: {}", ex.getMessage(), ex);
        return R.error(UserAuthCode.INVALID_PARAM);
    }
}
