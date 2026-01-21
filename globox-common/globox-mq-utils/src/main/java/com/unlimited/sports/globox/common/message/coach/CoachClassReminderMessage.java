package com.unlimited.sports.globox.common.message.coach;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 教练课程提醒延迟消息
 * 在教练确认订单后发送，延迟到课程开始前1小时投递
 * 消费时直接使用消息中的信息发送通知，无需查询数据库
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachClassReminderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    private Long orderNo;

    /**
     * 教练ID
     */
    private Long coachId;

    /**
     * 学员ID
     */
    private Long buyerId;

    /**
     * 教练姓名
     */
    private String coachName;

}
