package com.unlimited.sports.globox.common.handler;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.ApplicationCode;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.ValidCode;
import lombok.extern.log4j.Log4j2;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 统一异常处理
 *
 * @author dk
 * @since 2025/12/17 22:03
 */
@Log4j2
@RestControllerAdvice
public class GloboxGlobalExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(HttpServletRequest req, MissingServletRequestParameterException ex) {
        logWarn(req, ValidCode.MISSING_REQUEST_PARAM.getCode(), ex.getMessage());
        return R.error(ValidCode.MISSING_REQUEST_PARAM);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(HttpServletRequest req, ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(";"));
        logWarn(req, ValidCode.INVALID_REQUEST_PARAM.getCode(), detail);
        return R.<Void>error(ValidCode.INVALID_REQUEST_PARAM)
                .message(ValidCode.INVALID_REQUEST_PARAM.getMessage() + ":" + detail);
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(HttpServletRequest req, BindException ex) {
        String detail = ex.getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining(";"));
        logWarn(req, ValidCode.INVALID_REQUEST_PARAM.getCode(), detail);
        return R.<Void>error(ValidCode.INVALID_REQUEST_PARAM)
                .message(ValidCode.INVALID_REQUEST_PARAM.getMessage() + ":" + detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgNotValid(HttpServletRequest req, MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining(";"));
        logWarn(req, ValidCode.INVALID_REQUEST_PARAM.getCode(), detail);
        return R.<Void>error(ValidCode.INVALID_REQUEST_PARAM)
                .message(ValidCode.INVALID_REQUEST_PARAM.getMessage() + ":" + detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpServletRequest req, HttpMessageNotReadableException ex) {
        // JSON 格式问题：客户端问题
        logWarn(req, ValidCode.INVALID_REQUEST_PARAM.getCode(), "JSON parse error: " + ex.getMostSpecificCause().getMessage());
        return R.error(ValidCode.INVALID_REQUEST_PARAM);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleMaxUpload(HttpServletRequest req, MaxUploadSizeExceededException ex) {
        logWarn(req, ApplicationCode.FILE_SIZE_EXCEEDED.getCode(), ex.getMessage());
        return R.error(ApplicationCode.FILE_SIZE_EXCEEDED);
    }

    @ExceptionHandler(GloboxApplicationException.class)
    public R<Void> handleApp(HttpServletRequest req, GloboxApplicationException ex) {
        int code = ex.getCode();

        if (code == ApplicationCode.FAIL.getCode()) {
            // 只有 500：系统错误，打堆栈
            logError(req, code, ex.getMessage(), ex);
        } else {
            logWarn(req, code, ex.getMessage());
        }

        return R.error(ex);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleAny(HttpServletRequest req, Exception ex) {
        // 未知异常：视为 500
        logError(req, ApplicationCode.FAIL.getCode(), ex.getMessage(), ex);
        return R.error(ApplicationCode.FAIL);
    }

    // ------------------- logging helpers -------------------


    private void logInfo(HttpServletRequest req, int code, String msg) {
        log.info("biz: code={}, {} uri={} ip={} msg={}",
                code, req.getMethod(), uriWithQuery(req), getClientIp(req), msg);
    }

    private void logWarn(HttpServletRequest req, int code, String msg) {
        log.warn("warn: code={}, {} uri={} ip={} msg={}",
                code, req.getMethod(), uriWithQuery(req), getClientIp(req), msg);
    }

    private void logError(HttpServletRequest req, int code, String msg, Throwable ex) {
        log.error("error: code={}, {} uri={} ip={} msg={}",
                code, req.getMethod(), uriWithQuery(req), getClientIp(req), msg, ex);
    }

    private String uriWithQuery(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String qs = req.getQueryString();
        return (qs == null || qs.isBlank()) ? uri : (uri + "?" + qs);
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return req.getRemoteAddr();
    }
}