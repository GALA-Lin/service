package com.unlimited.sports.globox.payment.mapper;

import com.unlimited.sports.globox.common.enums.payment.PaymentStatusEnum;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 针对表【payments(支付信息表)】的数据库操作Mapper
 */
@Mapper
public interface PaymentsMapper extends BaseMapper<Payments> {
    @Update("""
        UPDATE payments
        SET trade_no = #{tradeNo},
            payment_status = #{paidStatus},
            payment_at = #{paymentAt},
            callback_at = #{callbackAt},
            callback_content = #{callbackContent},
            payment_at = NOW()
        WHERE id = #{id}
          AND payment_status = #{unpaidStatus}
    """)
    int updatePaidIfUnpaid(@Param("id") Long id,
            @Param("tradeNo") String tradeNo,
            @Param("paidStatus") PaymentStatusEnum paidStatus,
            @Param("unpaidStatus") PaymentStatusEnum unpaidStatus,
            @Param("paymentAt") LocalDateTime paymentAt,
            @Param("callbackAt") LocalDateTime callbackAt,
            @Param("callbackContent") String callbackContent);
}




