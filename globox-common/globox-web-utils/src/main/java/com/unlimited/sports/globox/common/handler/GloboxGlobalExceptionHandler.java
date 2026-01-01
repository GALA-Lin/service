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

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * 统一异常处理
 *
 * @author dk
 * @since 2025/12/17 22:03
 */
@Log4j2
@ControllerAdvice
public class GloboxGlobalExceptionHandler {

    /**
     * 处理 MissingServletRequestParameterException 异常
     * SpringMVC 参数不正确
     * 没有携带需要的参数
     */
    @ResponseBody
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public R<Void> missingServletRequestParameterExceptionHandler(HttpServletRequest req, MissingServletRequestParameterException ex) {
        log.error("An error occurred: {}", ex.getMessage(), ex);
        // 包装 R 结果
        return R.error(ValidCode.MISSING_REQUEST_PARAM);
    }


    /**
     * get 携带query参数与路径参数
     */
    @ResponseBody
    @ExceptionHandler(value = ConstraintViolationException.class)
    public R<Void> constraintViolationExceptionHandler(HttpServletRequest req, ConstraintViolationException ex) {
        log.error("An error occurred: {}", ex.getMessage(), ex);
        // 拼接错误
        StringBuilder detailMessage = new StringBuilder();
        for (ConstraintViolation<?> constraintViolation : ex.getConstraintViolations()) {
            // 使用 ; 分隔多个错误
            if (!detailMessage.isEmpty()) {
                detailMessage.append(";");
            }
            // 拼接内容到其中
            detailMessage.append(constraintViolation.getMessage());
        }
        // 包装 R 结果
        return R.<Void>error(ValidCode.INVALID_REQUEST_PARAM)
                .message(ValidCode.INVALID_REQUEST_PARAM.getMessage() + ":" + detailMessage);
    }


    /**
     * 表单参数校验
     */
    @ResponseBody
    @ExceptionHandler(value = BindException.class)
    public R<Void> bindExceptionHandler(HttpServletRequest req, BindException ex) {
        log.error("An error occurred: {}", ex.getMessage(), ex);
        // 拼接错误
        StringBuilder detailMessage = new StringBuilder();
        for (ObjectError objectError : ex.getAllErrors()) {
            // 使用 ; 分隔多个错误
            if (!detailMessage.isEmpty()) {
                detailMessage.append(";");
            }
            // 拼接内容到其中
            detailMessage.append(objectError.getDefaultMessage());
        }
        // 包装 R 结果
        return R.<Void>error(ValidCode.INVALID_REQUEST_PARAM)
                .message(ValidCode.INVALID_REQUEST_PARAM.getMessage() + ":" + detailMessage);
    }


    /**
     * post请求，json参数
     */
    @ResponseBody
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R<Void> methodArgumentNotValidExceptionHandler(HttpServletRequest req, MethodArgumentNotValidException ex) {
        log.error("An error occurred: {}", ex.getMessage(), ex);
        // 拼接错误
        StringBuilder detailMessage = new StringBuilder();
        for (ObjectError objectError : ex.getBindingResult().getAllErrors()) {
            // 使用 ; 分隔多个错误
            if (!detailMessage.isEmpty()) {
                detailMessage.append(";");
            }
            // 拼接内容到其中
            detailMessage.append(objectError.getDefaultMessage());
        }
        // 包装 R 结果
        return R.<Void>error(ValidCode.INVALID_REQUEST_PARAM)
                .message(ValidCode.INVALID_REQUEST_PARAM.getMessage() + ":" + detailMessage);
    }


    /**
     * 处理全局自定义异常
     *
     * @param ex exception
     */
    @ResponseBody
    @ExceptionHandler(GloboxApplicationException.class)
    public R<Void> applicationExceptionHandler(GloboxApplicationException ex) {
        log.error("An error occurred: {}", ex.getMessage(), ex);;
        // 使用公共的结果类封装返回结果, 这里我指定状态码为
        return R.error(ex);
    }


    /**
     * 处理其它 Exception 异常
     */
    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public R<Void> exceptionHandler(HttpServletRequest req, Exception ex) {
        log.error("An error occurred: {}", ex.getMessage(), ex);
        // 返回 ERROR R
        return R.error(ApplicationCode.FAIL);
    }
}
