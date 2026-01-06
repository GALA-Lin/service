package com.unlimited.sports.globox.model.social.dto;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.unlimited.sports.globox.model.social.entity.RallyActivityTypeEnum;
import com.unlimited.sports.globox.model.social.entity.RallyCostBearerEnum;
import com.unlimited.sports.globox.model.social.entity.RallyGenderLimitEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RallyPostsDto {

    /**
     * 约球宣言
     */

    private String rallyTitle;

    /**
     * 区域
     */
    @NotNull
    private String rallyRegion;

    /**
     * 球馆名称(可空)
     */

    private String rallyVenueName;

    /**
     * 场地名称(可空)
     */

    private String rallyCourtName;

    /**
     * 日期
     */
    @NotNull
    private LocalDate rallyEventDate;

    /**
     * 时间类型: 0=上午 1=下午 2=晚上
     */
    @NotNull
    private int rallyTimeType ;

    /**
     * 时间-开始
     */

    private LocalTime rallyStartTime;

    /**
     * 时间-结束
     */
    private LocalTime rallyEndTime;

    /**
     * 费用
     */
    @NotNull
    private BigDecimal rallyCost;

    /**
     * 承担方式: 0=发起人承担 1=AA分摊
     */
    @NotNull
    private int rallyCostBearer;

    /**
     * 活动类型: 0=不限 1=单打 2=双打
     */
    private int rallyActivityType = RallyActivityTypeEnum.UNLIMITED.getCode();

    /**
     * 性别限制: 0=不限 1=仅男生 2=仅女生
     */
    private int rallyGenderLimit = RallyGenderLimitEnum.NO_LIMIT.getCode();

    /**
     * NTRP最低水平(1.5-7.0)
     */
    @NotNull
    private double rallyNtrpMin;

    /**
     * NTRP最高水平(1.5-7.0)
     */
    @NotNull
    private double rallyNtrpMax;

    /**
     * 总人数
     */
    @NotNull
    private Integer rallyTotalPeople;
    /**
     * 剩余人数
     */
    @NotNull
    private Integer rallyRemainingPeople;
    /**
     * 备注
     */
    private String rallyNotes;

}
