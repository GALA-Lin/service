package com.unlimited.sports.globox.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.merchant.entity.VenueRefundRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @since 2025/12/31 11:10
 * 退款规则 Mapper
 */
@Mapper
public interface VenueRefundRuleMapper extends BaseMapper<VenueRefundRule> {

    /**
     * 查询商家的默认规则
     * @param merchantId 商家ID
     * @param venueId 场馆ID（可选，NULL表示商家默认规则）
     * @return 默认规则
     */
    @Select("<script>" +
            "SELECT * FROM venue_refund_rules " +
            "WHERE merchant_id = #{merchantId} " +
            "AND is_default = 1 " +
            "AND is_enabled = 1 " +
            "<if test='venueId != null'>" +
            "AND venue_id = #{venueId} " +
            "</if>" +
            "<if test='venueId == null'>" +
            "AND venue_id IS NULL " +
            "</if>" +
            "LIMIT 1" +
            "</script>")
    VenueRefundRule selectDefaultRule(@Param("merchantId") Long merchantId,
                                      @Param("venueId") Long venueId);

    /**
     * 统计使用该规则的场馆数量
     * @param ruleId 规则ID
     * @return 场馆数量
     */
    @Select("SELECT COUNT(*) FROM venues WHERE venue_refund_rule_id = #{ruleId}")
    Integer countVenuesByRuleId(@Param("ruleId") Long ruleId);
}

