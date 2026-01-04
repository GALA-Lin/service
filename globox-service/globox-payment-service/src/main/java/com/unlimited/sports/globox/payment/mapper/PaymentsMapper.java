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
    /**
     * 支付成功：UNPAID -> PAID（只会成功一次）
     */
    @Update("""
        UPDATE payments
        SET payment_status = #{paidCode},
            trade_no = #{tradeNo},
            payment_at = #{paymentAt},
            callback_at = #{callbackAt},
            callback_content = #{callbackContent},
            updated_at = NOW()
        WHERE id = #{id}
          AND payment_status = #{unpaidCode}
    """)
    int updatePaidIfUnpaid(@Param("id") Long id,
            @Param("tradeNo") String tradeNo,
            @Param("paymentAt") LocalDateTime paymentAt,
            @Param("callbackAt") LocalDateTime callbackAt,
            @Param("callbackContent") String callbackContent,
            @Param("paidCode") Integer paidCode,
            @Param("unpaidCode") Integer unpaidCode);

    /**
     * 交易关闭：UNPAID -> CLOSED（只会成功一次）
     */
    @Update("""
        UPDATE payments
        SET payment_status = #{closedCode},
            callback_at = #{callbackAt},
            callback_content = #{callbackContent},
            updated_at = NOW()
        WHERE id = #{id}
          AND payment_status = #{unpaidCode}
    """)
    int updateClosedIfUnpaid(@Param("id") Long id,
            @Param("callbackAt") LocalDateTime callbackAt,
            @Param("callbackContent") String callbackContent,
            @Param("closedCode") Integer closedCode,
            @Param("unpaidCode") Integer unpaidCode);

    /**
     * 部分退款：PAID -> PARTIALLY_REFUNDED（只会成功一次）
     * 你可以在退款服务里调用它
     */
    @Update("""
        UPDATE payments
        SET payment_status = #{partialCode},
            out_request_no = #{outRequestNo},
            callback_at = #{callbackAt},
            callback_content = #{callbackContent},
            updated_at = NOW()
        WHERE id = #{id}
          AND payment_status = #{paidCode}
    """)
    int updatePartiallyRefundedIfPaid(@Param("id") Long id,
            @Param("outRequestNo") String outRequestNo,
            @Param("callbackAt") LocalDateTime callbackAt,
            @Param("callbackContent") String callbackContent,
            @Param("partialCode") Integer partialCode,
            @Param("paidCode") Integer paidCode);

    /**
     * 仅追加回调信息（不改变业务状态）
     */
    @Update("""
        UPDATE payments
        SET callback_at = #{callbackAt},
            callback_content = #{callbackContent},
            updated_at = NOW()
        WHERE id = #{id}
    """)
    int updateCallbackOnly(@Param("id") Long id,
            @Param("callbackAt") LocalDateTime callbackAt,
            @Param("callbackContent") String callbackContent);


    /**
     * PAID -> PARTIALLY_REFUNDED（第一次部分退款）
     */
    @Update("""
        UPDATE payments
        SET payment_status = #{partialCode},
            out_request_no = #{outRequestNo},
            callback_at = #{callbackAt},
            callback_content = #{callbackContent},
            updated_at = NOW()
        WHERE id = #{id}
          AND payment_status = #{paidCode}
    """)
    int updatePartialIfPaid(@Param("id") Long id,
            @Param("outRequestNo") String outRequestNo,
            @Param("callbackAt") LocalDateTime callbackAt,
            @Param("callbackContent") String callbackContent,
            @Param("partialCode") Integer partialCode,
            @Param("paidCode") Integer paidCode);

    /**
     * PARTIALLY_REFUNDED -> PARTIALLY_REFUNDED（后续部分退款，仅更新回调记录）
     */
    @Update("""
        UPDATE payments
        SET out_request_no = #{outRequestNo},
            callback_at = #{callbackAt},
            callback_content = #{callbackContent},
            updated_at = NOW()
        WHERE id = #{id}
          AND payment_status = #{partialCode}
    """)
    int touchPartialIfPartial(@Param("id") Long id,
            @Param("outRequestNo") String outRequestNo,
            @Param("callbackAt") LocalDateTime callbackAt,
            @Param("callbackContent") String callbackContent,
            @Param("partialCode") Integer partialCode);

    /**
     * PAID / PARTIALLY_REFUNDED -> REFUND（全额退款完成）
     */
    @Update("""
        UPDATE payments
        SET payment_status = #{refundCode},
            out_request_no = #{outRequestNo},
            callback_at = #{callbackAt},
            callback_content = #{callbackContent},
            updated_at = NOW()
        WHERE id = #{id}
          AND payment_status IN (#{paidCode}, #{partialCode})
    """)
    int updateRefundedIfPaidOrPartial(@Param("id") Long id,
            @Param("outRequestNo") String outRequestNo,
            @Param("callbackAt") LocalDateTime callbackAt,
            @Param("callbackContent") String callbackContent,
            @Param("refundCode") Integer refundCode,
            @Param("paidCode") Integer paidCode,
            @Param("partialCode") Integer partialCode);
}