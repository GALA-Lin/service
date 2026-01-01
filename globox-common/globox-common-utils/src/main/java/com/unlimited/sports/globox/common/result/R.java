package com.unlimited.sports.globox.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一返回类型
 *
 * @author dk
 * @since 2025/12/17 22:07
 */
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 7735505903525411467L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据载体
     */
    private T data;

    private R() {
    }

    private R(ResultCode result) {
        this.code = result.getCode();
        this.message = result.getMessage();
    }

    public R<T> code(Integer code) {
        this.setCode(code);
        return this;
    }

    public R<T> message(String message) {
        this.setMessage(message);
        return this;
    }

    public R<T> data(T data) {
        this.setData(data);
        return this;
    }

    /**
     * 成功
     */
    public static <T> R<T> ok() {
        return new R<T>(ApplicationCode.SUCCESS);
    }

    /**
     * 成功
     */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<T>(ApplicationCode.SUCCESS);
        return r.data(data);
    }

    /**
     * 失败
     */
    public static <T> R<T> error() {
        return new R<T>(ApplicationCode.FAIL);
    }

    /**
     * 失败
     */
    public static <T> R<T> error(ResultCode code) {
        return new R<T>(code);
    }

    /**
     * 失败
     */
    public static <T> R<T> error(GloboxApplicationException ex) {
        R<T> r = new R<T>();
        r.setCode(ex.getCode());
        r.setMessage(ex.getMessage());
        return r;
    }

    /**
     * 处理是否达到预期
     *
     * @return boolean
     */
    public boolean success() {
        return code != null && code == 200;
    }

    public Integer getCode() {
        return code;
    }

    private void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    private void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    private void setData(T data) {
        this.data = data;
    }


}