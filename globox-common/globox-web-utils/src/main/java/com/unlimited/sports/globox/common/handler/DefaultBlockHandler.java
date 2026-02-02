package com.unlimited.sports.globox.common.handler;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlimited.sports.globox.common.result.ApplicationCode;
import com.unlimited.sports.globox.common.result.CustomMessageCode;
import com.unlimited.sports.globox.common.result.R;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.unlimited.sports.globox.common.result.ResultCode;
import com.unlimited.sports.globox.common.utils.LimitUtils;
import com.unlimited.sports.globox.common.utils.RequestContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.nio.charset.StandardCharsets;

import static com.unlimited.sports.globox.common.constants.ResponseHeaderConstants.BUSINESS_CODE;

/**
 * 限流默认处理器
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class DefaultBlockHandler implements BlockExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, BlockException e) throws Exception {
        httpServletResponse.setHeader(BUSINESS_CODE, ApplicationCode.FLOW.getCode().toString());
        httpServletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ResultCode resultCode = LimitUtils.getLimitTypeByException(e);

        R<Void> body = R.error(resultCode);

        log.warn("Sentinel block: type={}, uri={}, method={}, ip={}",
                resultCode.name(),
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod(),
                RequestContextHolder.getCurrentRequestIp());

        httpServletResponse.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
