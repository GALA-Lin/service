package com.unlimited.sports.globox.common.exception;

import com.unlimited.sports.globox.common.result.ApplicationCode;
import com.unlimited.sports.globox.common.result.ResultCode;
import lombok.Getter;


/**
 * 业务异常类
 *
 * @author dk
 * @since 2025/12/17 22:02
 */
@Getter
public class GloboxApplicationException extends RuntimeException {

    private final int code;

    public GloboxApplicationException(String message) {
        super(message);
        this.code = ApplicationCode.FAIL.getCode();
    }

    public GloboxApplicationException(Exception e) {
        super(ApplicationCode.FAIL.getMessage(), e);
        this.code = ApplicationCode.FAIL.getCode();
    }

    public GloboxApplicationException(int code, String message) {
        super(message);
        this.code = code;
    }


    public GloboxApplicationException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public GloboxApplicationException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
    }

    public GloboxApplicationException(String message, Throwable cause) {
        super(message, cause);
        this.code = ApplicationCode.FAIL.getCode();
    }
}
