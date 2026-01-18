package com.unlimited.sports.globox.model.merchant.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Builder;
import lombok.Data;

/**
 * @since 2026/1/18 13:56
 *
 */
@Data
public class MerchantInfoVo {

    /**
     * 商家ID
     */
    private Long merchantId;

    /**
     * 商家名称
     */
    private String merchantName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 当前登陆的员工用户ID
     */
    private Long userId;

    /**
     * 员工名称
     */
    @TableField("display_name")
    private String displayName;

    /**
     * 员工职位名称：店长、前台、运营等
     */
    private String jobTitle;


}
