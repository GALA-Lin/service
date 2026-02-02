package com.unlimited.sports.globox.common.handler;

import com.unlimited.sports.globox.common.result.ApplicationCode;
import com.unlimited.sports.globox.common.result.R;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletResponse;

import static com.unlimited.sports.globox.common.constants.ResponseHeaderConstants.BUSINESS_CODE;

@RestControllerAdvice
public class BizCodeHeaderAdvice implements ResponseBodyAdvice<R<?>> {

    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        // 只有返回值类型是 R 才生效（更精确，也更“自说明”）
        return R.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public R<?> beforeBodyWrite(R<?> body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        if (!MediaType.APPLICATION_JSON.includes(selectedContentType)) {
            return body;
        }

        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

        Integer bizCode = body == null ? null : body.getCode();
        servletResponse.setHeader(BUSINESS_CODE, ObjectUtils.isEmpty(bizCode)
                ? ApplicationCode.UNKNOW.getCode().toString() : bizCode.toString()
        );
        return body;
    }
}