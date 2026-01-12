package com.unlimited.sports.globox.payment.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.payment.dto.GetPaymentStatusRequestDto;
import com.unlimited.sports.globox.model.payment.dto.SubmitRequestDto;
import com.unlimited.sports.globox.model.payment.vo.GetPaymentStatusResultVo;
import com.unlimited.sports.globox.model.payment.vo.SubmitResultVo;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 支付模块 controller
 */
@Slf4j
@Validated
@RestController
@RequestMapping("payments")
public class PaymentController {

    @Autowired
    private PaymentsService paymentsService;


    /**
     * 下单
     *
     * @param orderNo 订单号
     * @return orderStr / prepayId
     */
    @PostMapping("/submit/{orderNo}")
    public R<SubmitResultVo> submit(
            @PathVariable("orderNo") Long orderNo,
            @RequestHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE) String clientTypeStr,
            @RequestHeader(value = RequestHeaderConstants.HEADER_THIRD_PARTY_OPENID, required = false) String openId,
            @RequestBody @Valid SubmitRequestDto dto) {
        dto.setOrderNo(orderNo);

        ClientType clientType = ClientType.fromValue(clientTypeStr);
        dto.setClientType(clientType);
        dto.setOpenId(openId);
        SubmitResultVo submitResultVo = paymentsService.submit(dto);
        return R.ok(submitResultVo);
    }


    /**
     * 获取指定外部交易号的支付状态。
     *
     * @param outTradeNo 外部交易号
     * @param dto 携带支付类型的 dto 信息
     * @return 包含支付状态信息的结果对象
     */
    @GetMapping("status/{outTradeNo}")
    public R<GetPaymentStatusResultVo> getPaymentStatus(@PathVariable String outTradeNo, @ModelAttribute @Valid GetPaymentStatusRequestDto dto) {
        PaymentTypeEnum paymentType = PaymentTypeEnum.from(dto.getPaymentTypeCode());
        GetPaymentStatusResultVo paymentStatus = paymentsService.getPaymentStatus(outTradeNo, paymentType);
        return R.ok(paymentStatus);
    }
}
