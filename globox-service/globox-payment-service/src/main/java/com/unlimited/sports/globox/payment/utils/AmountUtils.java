package com.unlimited.sports.globox.payment.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额处理
 */
public final class AmountUtils {

    /**
     * 通过将小数点向右移动两位，四舍五入到最接近的整数，然后转换为整数，将BigDecimal金额转换为整数。
     *
     * @param amount 要转换的BigDecimal值
     * @return 转换后的整数值
     */
    public static Integer toInteger(BigDecimal amount) {
       return amount
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }


    public static Long toLong(BigDecimal amount) {
        return amount
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    public static BigDecimal toBigDecimal(Long amount) {
        if (amount == null) {
            return null;
        }
        return BigDecimal.valueOf(amount)
                .movePointLeft(2)
                .setScale(2, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal toBigDecimal(Integer amount) {
        if (amount == null) {
            return null;
        }
        return BigDecimal.valueOf(amount.longValue())
                .movePointLeft(2)
                .setScale(2, RoundingMode.UNNECESSARY);
    }
}
