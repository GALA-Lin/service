package com.unlimited.sports.globox.dubbo.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 教练注册分账方
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterProfitSharingCoachReceiverDto implements Serializable {

    private String realName;

    private String account;
}
