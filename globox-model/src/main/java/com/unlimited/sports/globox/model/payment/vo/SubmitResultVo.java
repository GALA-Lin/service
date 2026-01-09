package com.unlimited.sports.globox.model.payment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 提交支付 result vo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitResultVo implements Serializable {

    private String orderStr;

    private String outTradeNo;
}
