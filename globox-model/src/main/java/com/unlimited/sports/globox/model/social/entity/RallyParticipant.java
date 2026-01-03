package com.unlimited.sports.globox.model.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 参与人表实体类
 * 对应数据库表：rally_participant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("rally_participant")
public class RallyParticipant implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键，参与记录ID
     */
    @TableId(value = "rally_participant_id", type = IdType.AUTO)
    private Long rallyParticipantsId;

    /**
     * 球贴id
     */
    @TableField("rally_post_id")
    private Long rallyPostId;

    /**
     * 申请人id（参与者ID）
     */
    @TableField("participant_id")
    private Long participantId;

    /**
     * 参与者的网球等级
     */
    @TableField("user_ntrp")
    private double userNtrp;

    /**
     * 加入时间
     */
    @TableField("joined_at")
    private LocalDateTime joinedAt;

    /**
     * 是否主动取消
     */
    @TableField("is_voluntarily_cancel")
    private boolean isVoluntarilyCancel;
}