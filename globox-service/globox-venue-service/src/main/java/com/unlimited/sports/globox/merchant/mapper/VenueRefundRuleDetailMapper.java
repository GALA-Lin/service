package com.unlimited.sports.globox.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.merchant.entity.VenueRefundRuleDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 退款规则明细 Mapper
 *
 * @since 2025-12-30
 */
@Mapper
public interface VenueRefundRuleDetailMapper extends BaseMapper<VenueRefundRuleDetail> {

    /**
     * 根据规则ID查询所有明细（按排序号排序）
     *
     * @param ruleId 规则ID
     * @return 明细列表
     */
    @Select("SELECT * FROM venue_refund_rule_details " +
            "WHERE venue_refund_rule_id = #{ruleId} " +
            "AND deleted = 0 " +  // 新增：过滤已删除数据
            "ORDER BY sort_order_num ASC, min_hours_before DESC")
    List<VenueRefundRuleDetail> selectByRuleId(@Param("ruleId") Long ruleId);

    /**
     * 批量插入明细
     *
     * @param details 明细列表
     * @return 插入行数
     */
    int batchInsert(@Param("details") List<VenueRefundRuleDetail> details);

    /**
     * 删除规则的所有明细
     *
     * @param ruleId 规则ID
     * @return 删除行数
     */
    int deleteByRuleId(@Param("ruleId") Long ruleId);
}
