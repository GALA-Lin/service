package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import com.unlimited.sports.globox.venue.service.IVenueBookingSlotRecordService;
import org.springframework.stereotype.Service;

/**
 * 场馆槽位记录 Service 实现
 *
 * @author globox
 */
@Service
public class VenueBookingSlotRecordServiceImpl extends ServiceImpl<VenueBookingSlotRecordMapper, VenueBookingSlotRecord>
        implements IVenueBookingSlotRecordService {
}
