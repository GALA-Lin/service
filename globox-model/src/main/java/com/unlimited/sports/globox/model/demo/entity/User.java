package com.unlimited.sports.globox.model.demo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.model.base.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表
 * @TableName user
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value ="user")
@Data
public class User extends BaseEntity implements Serializable {
    /**
     * 用户名（唯一）
     */
    private String username;


    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 用户状态：1-正常 0-禁用
     */
    private Integer status;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}