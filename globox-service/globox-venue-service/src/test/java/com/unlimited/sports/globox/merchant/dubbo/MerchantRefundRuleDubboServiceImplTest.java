package com.unlimited.sports.globox.merchant.dubbo;

import com.unlimited.sports.globox.dubbo.merchant.dto.*;
import com.unlimited.sports.globox.merchant.mapper.VenueRefundRuleDetailMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueRefundRuleMapper;
import com.unlimited.sports.globox.model.merchant.entity.VenueRefundRule;
import com.unlimited.sports.globox.model.merchant.entity.VenueRefundRuleDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantRefundRuleDubboServiceImplTest {

    @Mock
    private VenueRefundRuleMapper refundRuleMapper;

    @Mock
    private VenueRefundRuleDetailMapper refundRuleDetailMapper;

    @InjectMocks
    private MerchantRefundRuleDubboServiceImpl refundRuleService;

    private MerchantRefundRuleJudgeRequestDto baseRequest;
    private final Long merchantId = 100L;
    private final Long venueId = 200L;
    private final LocalDateTime eventStartTime = LocalDateTime.of(2026, 1, 10, 10, 0); // 1月10日 10:00

    @BeforeEach
    void setUp() {
        baseRequest = MerchantRefundRuleJudgeRequestDto.builder()
                .venueId(venueId)
                .eventStartTime(eventStartTime)
                .userId(1L)
                .orderTime(eventStartTime.minusDays(1))
                .build();
    }


    @Test
    @DisplayName("退款申请时间晚于活动开始时间 - 应拒绝退款")
    void judge_ApplyTimeAfterEvent_ShouldReturnCannotRefund() {
        // 申请时间：10:01
        baseRequest.setRefundApplyTime(eventStartTime.plusMinutes(1));

        MerchantRefundRuleJudgeResultVo result = refundRuleService.judgeApplicableRefundRule(baseRequest);

        assertFalse(result.isCanRefund());
        assertEquals("退款申请时间晚于活动开始时间，不支持退款", result.getReason());
    }

    @Test
    @DisplayName("未查到任何退款规则 - 应拒绝退款")
    void judge_NoRuleFound_ShouldReturnCannotRefund() {
        baseRequest.setRefundApplyTime(eventStartTime.minusHours(10));

        // Mock 场馆和商家规则都为空
        when(refundRuleMapper.selectDefaultRule(merchantId, venueId)).thenReturn(null);
        when(refundRuleMapper.selectDefaultRule(merchantId, null)).thenReturn(null);

        MerchantRefundRuleJudgeResultVo result = refundRuleService.judgeApplicableRefundRule(baseRequest);

        assertFalse(result.isCanRefund());
        assertTrue(result.getReason().contains("未找到可用的退款规则"));
    }

    @Test
    @DisplayName("匹配到无上限的时间范围（例如提前48小时以上） - 应返回对应比例")
    void judge_MatchNoLimitRange_ShouldReturnSuccess() {
        // 提前 50 小时申请
        baseRequest.setRefundApplyTime(eventStartTime.minusHours(50));

        // Mock 规则
        VenueRefundRule rule = createMockRule(1L);
        when(refundRuleMapper.selectDefaultRule(merchantId, venueId)).thenReturn(rule);

        // Mock 明细：提前48小时以上退100%
        VenueRefundRuleDetail detail = new VenueRefundRuleDetail();
        detail.setVenueRefundRuleDetailId(10L);
        detail.setMinHoursBefore(48);
        detail.setMaxHoursBefore(null); // 无上限
        detail.setRefundPercentage(new BigDecimal("100"));
        detail.setSortOrderNum(1);

        when(refundRuleDetailMapper.selectByRuleId(1L)).thenReturn(Collections.singletonList(detail));

        MerchantRefundRuleJudgeResultVo result = refundRuleService.judgeApplicableRefundRule(baseRequest);

        assertTrue(result.isCanRefund());
        assertEquals(0, new BigDecimal("100").compareTo(result.getRefundPercentage()));
    }

    @Test
    @DisplayName("匹配到有上限的阶梯范围（例如提前24-48小时） - 应返回对应比例")
    void judge_MatchRangeWithLimit_ShouldReturnSuccess() {
        // 提前 30 小时申请
        baseRequest.setRefundApplyTime(eventStartTime.minusHours(30));

        VenueRefundRule rule = createMockRule(1L);
        when(refundRuleMapper.selectDefaultRule(merchantId, venueId)).thenReturn(rule);

        // 定义两个阶梯
        VenueRefundRuleDetail step1 = createDetail(10L, 48, null, 100, 1);  // 48h+ 退100%
        VenueRefundRuleDetail step2 = createDetail(11L, 24, 48, 80, 2);    // 24-48h 退80%

        when(refundRuleDetailMapper.selectByRuleId(1L)).thenReturn(Arrays.asList(step1, step2));

        MerchantRefundRuleJudgeResultVo result = refundRuleService.judgeApplicableRefundRule(baseRequest);

        assertTrue(result.isCanRefund());
        assertEquals(0, new BigDecimal("80").compareTo(result.getRefundPercentage()));
    }

    @Test
    @DisplayName("匹配到阶梯但退款比例为0 - 应返回不可退款及描述原因")
    void judge_MatchZeroPercentage_ShouldReturnCannotRefundWithReason() {
        // 提前 5 小时申请
        baseRequest.setRefundApplyTime(eventStartTime.minusHours(5));

        VenueRefundRule rule = createMockRule(1L);
        when(refundRuleMapper.selectDefaultRule(merchantId, venueId)).thenReturn(rule);

        VenueRefundRuleDetail step = createDetail(12L, 0, 24, 0, 1);
        step.setRefundRuleDetailDesc("开场前24小时内不支持退款");

        when(refundRuleDetailMapper.selectByRuleId(1L)).thenReturn(Collections.singletonList(step));

        MerchantRefundRuleJudgeResultVo result = refundRuleService.judgeApplicableRefundRule(baseRequest);

        assertFalse(result.isCanRefund());
        assertEquals("开场前24小时内不支持退款", result.getReason());
    }

    // --- 辅助方法 ---

    private VenueRefundRule createMockRule(Long id) {
        VenueRefundRule rule = new VenueRefundRule();
        rule.setVenueRefundRuleId(id);
        rule.setMerchantId(merchantId);
        rule.setVenueId(venueId);
        rule.setIsEnabled(1);
        rule.setIsDefault(1);
        return rule;
    }

    private VenueRefundRuleDetail createDetail(Long id, Integer min, Integer max, int percentage, int sort) {
        VenueRefundRuleDetail detail = new VenueRefundRuleDetail();
        detail.setVenueRefundRuleDetailId(id);
        detail.setMinHoursBefore(min);
        detail.setMaxHoursBefore(max);
        detail.setRefundPercentage(new BigDecimal(percentage));
        detail.setSortOrderNum(sort);
        detail.setHandlingFeePercentage(BigDecimal.ZERO);
        return detail;
    }
}