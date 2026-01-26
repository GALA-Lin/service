package com.unlimited.sports.globox.common.message.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单自动确认消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAutoCompleteMessage implements Serializable {
    private Long orderNo;

    private Integer retryCount;


    public void incrementRetryCount(){
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        this.retryCount++;
    }
}
