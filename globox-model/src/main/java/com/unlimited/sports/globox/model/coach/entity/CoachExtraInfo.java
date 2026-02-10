package com.unlimited.sports.globox.model.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @since 2026/2/9 17:20
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("coach_extra_info")
public class CoachExtraInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "coach_user_id", type = IdType.AUTO)
    private Long coachUserId;

    @TableField(value = "display_real_name")
    private Boolean displayRealName;
}
