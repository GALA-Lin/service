package com.unlimited.sports.globox.model.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户资料表
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Data
@TableName("user_profile")
public class UserProfile {
    
    /**
     * 资料表主键（自增ID）
     */
    @TableId(value = "profile_id", type = IdType.AUTO)
    private Long profileId;
    
    /**
     * 用户ID（与auth_user.user_id一对一，主键）
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 用户头像URL
     */
    private String avatarUrl;
    
    /**
     * 球星卡肖像URL（前端直传）
     */
    private String portraitUrl;
    
    /**
     * 用户昵称
     */
    private String nickName;
    
    /**
     * 个性签名
     */
    private String signature;
    
    /**
     * 性别
     */
    private Gender gender;
    
    /**
     * 球龄
     */
    private Integer sportsYears;
    
    /**
     * 网球水平
     */
    private BigDecimal ntrp;
    
    /**
     * 持拍手
     */
    private PreferredHand preferredHand;
    
    /**
     * 主力球拍
     */
    private String mainBattleShot;
    
    /**
     * 球风标签
     */
    private String styleTags;
    
    /**
     * 常驻区域
     */
    private String homeDistrict;
    
    /**
     * 力量
     */
    private Integer power;
    
    /**
     * 速度
     */
    private Integer speed;
    
    /**
     * 截击
     */
    private Integer volley;
    
    /**
     * 耐力
     */
    private Integer stamina;
    
    /**
     * 心理
     */
    private Integer mental;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 性别枚举
     */
    public enum Gender {
        MALE,
        FEMALE,
        OTHER
    }
    
    /**
     * 持拍手枚举
     */
    public enum PreferredHand {
        LEFT,
        RIGHT
    }
}
