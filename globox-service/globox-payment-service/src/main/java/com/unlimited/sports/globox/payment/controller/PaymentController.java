package com.unlimited.sports.globox.payment.controller;

import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.payment.dto.SubmitRequestDto;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
            @ModelAttribute SubmitRequestDto dto) {
        dto.setOrderNo(orderNo);
        String orderStr = paymentsService.submit(dto);
        return R.ok(orderStr);
    }
}
