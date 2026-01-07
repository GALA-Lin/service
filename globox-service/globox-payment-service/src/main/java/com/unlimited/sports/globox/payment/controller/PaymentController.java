package com.unlimited.sports.globox.payment.controller;

import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.enums.payment.PaymentClientTypeEnum;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.payment.dto.SubmitRequestDto;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 支付模块 controller
 */
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
    public R<String> submit(
            @PathVariable("orderNo") Long orderNo,
            @ModelAttribute @Valid SubmitRequestDto dto) {
        dto.setOrderNo(orderNo);
        String orderStr = paymentsService.submit(dto);
        return R.ok(orderStr);
    }


    /**
     * 下单
     */
    @Deprecated
    @PostMapping("alipay/submit/{orderNo}")
    public R<String> submit(
            @PathVariable("orderNo") Long orderNo) {
        SubmitRequestDto requestDto = SubmitRequestDto.builder()
                .orderNo(orderNo)
                .paymentTypeCode(PaymentTypeEnum.ALIPAY.getCode())
                .clientTypeCode(PaymentClientTypeEnum.APP.getCode())
                .build();
        String submit = paymentsService.submit(requestDto);
        return R.ok(submit);

    }
}
