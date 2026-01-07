package com.unlimited.sports.globox.venue.dto;

import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 槽位预订上下文
 * 封装根据槽位ID查询到的槽位、场地、场馆完整信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotBookingContext {

    /**
     * 槽位模板列表
     */
    private List<VenueBookingSlotTemplate> templates;

    /**
     * 场地列表
     */
    private List<Court> courts;

    /**
     * 场馆信息
     */
    private Venue venue;

    /**
     * 场地ID到场地名���的映射
     */
    private Map<Long, String> courtNameMap;

    /**
     * 场地ID���场地对象的映射
     */
    private Map<Long, Court> courtMap;
}
