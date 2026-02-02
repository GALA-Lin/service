package com.unlimited.sports.globox.venue.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "away")
@Data
public class AwayConfig {

    private Booking booking;

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }




    @Data
    public static class Booking{
        /**
         * 是否开启?
         */
        private Boolean open;

        /**
         * 可预定away球馆白名单
         */
        private List<Long> whiteListIds;


        /**
         * 记录在第三方中的预订人手机号
         */
        private String bookingUserPhone;


        /**
         * 记录在第三方中的预订人姓名
         */
        private String bookingUserName;
        /**
         * 是否所有人可以预定
         */
        private boolean all = false;
        /**
         * 测试金额
         */
        private BigDecimal testAmount = new BigDecimal("0.01");


        /**
         * 是否使用测试金额
         */
        private boolean useTestAmount = false;
    }
}