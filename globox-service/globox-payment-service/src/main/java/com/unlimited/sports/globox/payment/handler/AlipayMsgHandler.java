package com.unlimited.sports.globox.payment.handler;

import com.alipay.api.msg.MsgHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付宝长连接消息处理器
 */
@Slf4j
public class AlipayMsgHandler implements MsgHandler {
    @Override
    public void onMessage(String msgApi, String msgId, String bizContent) {
        log.info("receive message. msgApi:{} msgId:{} bizContent:{}", msgApi, msgId, bizContent);
    }
}
