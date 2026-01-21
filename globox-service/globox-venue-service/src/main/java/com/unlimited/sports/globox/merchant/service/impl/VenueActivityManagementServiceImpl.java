package com.unlimited.sports.globox.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.lock.RedisDistributedLock;
import com.unlimited.sports.globox.common.result.VenueCode;
import com.unlimited.sports.globox.common.utils.IdGenerator;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.MerchantMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.merchant.service.VenueActivityManagementService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Merchant;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.entity.VenueStaff;
import com.unlimited.sports.globox.model.merchant.vo.ActivityCreationResultVo;
import com.unlimited.sports.globox.model.venue.dto.CreateActivityDto;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.ActivityType;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivitySlotLock;
import com.unlimited.sports.globox.model.venue.enums.BookingSlotStatus;
import com.unlimited.sports.globox.model.venue.enums.OrganizerTypeEnum;
import com.unlimited.sports.globox.model.venue.enums.VenueActivityStatusEnum;
import com.unlimited.sports.globox.venue.constants.BookingCacheConstants;
import com.unlimited.sports.globox.venue.mapper.ActivityTypeMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivitySlotLockMapper;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotTemplateMapper;
import com.unlimited.sports.globox.venue.service.IVenueActivitySlotLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 活动管理Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VenueActivityManagementServiceImpl implements VenueActivityManagementService {

    private final VenueBookingSlotTemplateMapper slotTemplateMapper;
    private final VenueActivitySlotLockMapper activitySlotLockMapper;
    private final VenueBookingSlotRecordMapper slotRecordMapper;
    private final VenueActivityMapper venueActivityMapper;
    private final ActivityTypeMapper activityTypeMapper;
    private final VenueStaffMapper venueStaffMapper;
    private final MerchantMapper merchantMapper;
    private final CourtMapper courtMapper;
    private final VenueMapper venueMapper;
    private final RedisDistributedLock redisDistributedLock;
    private final IVenueActivitySlotLockService activitySlotLockService;
    private final IdGenerator idGenerator;  // 注入雪花算法ID生成器

    /**
     * 自注入，用于解决@Transactional自调用问题
     * 使用@Lazy避免循环依赖警告
     */
    @Autowired
    @Lazy
    private VenueActivityManagementServiceImpl self;

    @Override
    public ActivityCreationResultVo createActivity(CreateActivityDto dto, MerchantAuthContext context) {
        log.info("创建活动 - employeeId: {}, role: {}, activityName: {}, slotTemplateIds: {}",
                context.getEmployeeId(), context.getRole(), dto.getActivityName(), dto.getSlotTemplateIds());

        // 根据MerchantAuthContext的role确定组织者类型和名称
        String organizerName;
        OrganizerTypeEnum organizerType;
        Long organizerId;

        if (context.isOwner()) {
            // 老板 - 使用Merchant实体的merchantName
            organizerType = OrganizerTypeEnum.OWNER;
            organizerId = context.getMerchantId();
            Merchant merchant = merchantMapper.selectById(context.getMerchantId());
            if (merchant == null) {
                log.warn("未找到商家信息 - merchantId: {}", context.getMerchantId());
                throw new GloboxApplicationException("商家信息不存在");
            }
            organizerName = merchant.getMerchantName() != null ? merchant.getMerchantName() : "未知商家";
        } else {
            // 员工 - 使用VenueStaff的displayName
            organizerType = OrganizerTypeEnum.STAFF;
            organizerId = context.getEmployeeId();
            VenueStaff staff = venueStaffMapper.selectById(context.getEmployeeId());
            if (staff == null) {
                log.warn("未找到员工信息 - employeeId: {}", context.getEmployeeId());
                throw new GloboxApplicationException("员工信息不存在");
            }
            organizerName = staff.getDisplayName() != null ? staff.getDisplayName() : "未命名,ID:"+staff.getUserId();
        }

        // 查询槽位模板
        List<VenueBookingSlotTemplate> templates = slotTemplateMapper.selectBatchIds(dto.getSlotTemplateIds());
        if (templates.size() != dto.getSlotTemplateIds().size()) {
            log.warn("部分槽位模板不存在 - 请求{}个，找到{}个", dto.getSlotTemplateIds().size(), templates.size());
            throw new GloboxApplicationException(VenueCode.SLOT_TEMPLATE_NOT_EXIST);
        }

        // 验证槽位是否来自同一场地
        Long courtId = templates.get(0).getCourtId();
        boolean allSameCourt = templates.stream()
                .allMatch(template -> template.getCourtId().equals(courtId));
        if (!allSameCourt) {
            log.warn("槽位来自不同场地，不允许创建活动");
            throw new GloboxApplicationException("槽位必须来自同一场地");
        }

        // 验证槽位是否连续
        validateSlotContinuity(templates);

        // 查询活动类型
        ActivityType activityType = activityTypeMapper.selectById(dto.getActivityTypeId());
        if (activityType == null) {
            log.warn("活动类型不存在 - activityTypeId: {}", dto.getActivityTypeId());
            throw new GloboxApplicationException("活动类型不存在");
        }

        // 提取开始时间和结束时间
        LocalTime startTime = templates.stream()
                .map(VenueBookingSlotTemplate::getStartTime)
                .min(LocalTime::compareTo)
                .orElseThrow(() -> new IllegalStateException("无法确定活动开始时间"));

        LocalTime endTime = templates.stream()
                .map(VenueBookingSlotTemplate::getEndTime)
                .max(LocalTime::compareTo)
                .orElseThrow(() -> new IllegalStateException("无法确定活动结束时间"));

        // 验证活动日期和时间不能是过去的时间
        validateActivityDateTime(dto.getActivityDate(), startTime);

        // 获取场馆ID
        Court court = courtMapper.selectOne(
                new LambdaQueryWrapper<Court>()
                        .eq(Court::getCourtId, courtId)
                        .select(Court::getVenueId)
        );
        if (court == null || court.getVenueId() == null) {
            log.warn("无法获取场馆ID - courtId: {}", courtId);
            throw new GloboxApplicationException("无法获取场馆ID");
        }
        Long venueId = court.getVenueId();

        // 【新增】生成活动批次ID（使用雪花算法）
        Long merchantBatchId = idGenerator.nextId();
        log.info("生成活动批次ID: {} - organizerId: {}, organizerType: {}",
                merchantBatchId, organizerId, organizerType);

        // 验证槽位是否被其他活动占用（快速失败，减少锁竞争）
        validateSlotNotOccupiedByActivity(dto.getSlotTemplateIds(), dto.getActivityDate());

        // 验证槽位是否被订场占用（快速失败，减少锁竞争）
        validateSlotNotOccupiedByBooking(dto.getSlotTemplateIds(), dto.getActivityDate());

        // 构建分布式锁键列表（与用户订场使用相同的锁键格式）
        List<String> lockKeys = dto.getSlotTemplateIds().stream()
                .map(slotId -> buildLockKey(slotId, dto.getActivityDate()))
                .collect(Collectors.toList());

        List<RLock> locks = null;

        try {
            // 批量获取所有槽位的分布式锁
            locks = redisDistributedLock.tryLockMultiple(lockKeys, 1, -1L, TimeUnit.SECONDS);
            if (locks == null) {
                throw new GloboxApplicationException("该时段正在被操作，请稍后重试");
            }
            log.info("成功获取分布式锁 - organizerId: {}, organizerType: {}, 槽位数: {}",
                    organizerId, organizerType, dto.getSlotTemplateIds().size());

            // 在事务内执行活动创建（通过self调用以确保事务生效）
            VenueActivity activity = self.createActivityWithinTransaction(
                    dto, organizerId, organizerType, organizerName,
                    venueId, courtId, activityType, startTime, endTime, merchantBatchId
            );

            log.info("活动创建成功 - activityId: {}, organizerId: {}, organizerName: {}, batchId: {}",
                    activity.getActivityId(), organizerId, organizerName, merchantBatchId);

            // 构建返回结果
            return buildActivityCreationResult(activity, court, templates, dto, merchantBatchId);

        } finally {
            // 释放锁（无论事务成功还是失败）
            if (locks != null) {
                redisDistributedLock.unlockMultiple(locks);
                log.info("【锁包事务】释放分布式锁 - organizerId: {}, organizerType: {}",
                        organizerId, organizerType);
            }
        }
    }

    /**
     * 构建活动创建结果
     */
    private ActivityCreationResultVo buildActivityCreationResult(
            VenueActivity activity,
            Court court,
            List<VenueBookingSlotTemplate> templates,
            CreateActivityDto dto,
            Long merchantBatchId) {

        // 查询场馆信息
        Venue venue = venueMapper.selectById(activity.getVenueId());

        // 构建槽位列表
        List<ActivityCreationResultVo.ActivitySlotVo> occupiedSlots = templates.stream()
                .sorted(Comparator.comparing(VenueBookingSlotTemplate::getStartTime))
                .map(template -> ActivityCreationResultVo.ActivitySlotVo.builder()
                        .templateId(template.getBookingSlotTemplateId())
                        .slotType(2) // 活动槽位
                        .startTime(template.getStartTime())
                        .endTime(template.getEndTime())
                        .price(activity.getUnitPrice())
                        .isAvailable(activity.getCurrentParticipants() < activity.getMaxParticipants())
                        .status(activity.getCurrentParticipants() >= activity.getMaxParticipants() ? 2 : 1)
                        .statusDesc(activity.getCurrentParticipants() >= activity.getMaxParticipants()
                                ? "活动已满员" : "可报名")
                        .activityId(activity.getActivityId())
                        .merchantBatchId(merchantBatchId)
                        .build())
                .collect(Collectors.toList());

        // 获取活动状态描述
        VenueActivityStatusEnum statusEnum = VenueActivityStatusEnum.fromValue(activity.getStatus());
        String statusDesc = statusEnum != null ? statusEnum.getDesc() : "未知";

        return ActivityCreationResultVo.builder()
                .activityId(activity.getActivityId())
                .merchantBatchId(merchantBatchId)
                .activityName(activity.getActivityName())
                .activityTypeId(activity.getActivityTypeId())
                .activityTypeDesc(activity.getActivityTypeDesc())
                .activityDate(activity.getActivityDate())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .venueId(activity.getVenueId())
                .venueName(venue != null ? venue.getName() : null)
                .courtId(activity.getCourtId())
                .courtName(court.getName())
                .maxParticipants(activity.getMaxParticipants())
                .currentParticipants(activity.getCurrentParticipants())
                .unitPrice(activity.getUnitPrice())
                .description(activity.getDescription())
                .imageUrls(activity.getImageUrls())
                .registrationDeadline(activity.getRegistrationDeadline())
                .organizerId(activity.getOrganizerId())
                .organizerType(activity.getOrganizerType())
                .organizerName(activity.getOrganizerName())
                .contactPhone(activity.getContactPhone())
                .minNtrpLevel(activity.getMinNtrpLevel())
                .status(activity.getStatus())
                .statusDesc(statusDesc)
                .occupiedSlots(occupiedSlots)
                .build();
    }

    /**
     * 在事务内执行活动创建（只包含需要并发保护的操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public VenueActivity createActivityWithinTransaction(
            CreateActivityDto dto,
            Long organizerId,
            OrganizerTypeEnum organizerType,
            String organizerName,
            Long venueId,
            Long courtId,
            ActivityType activityType,
            LocalTime startTime,
            LocalTime endTime,
            Long merchantBatchId) {

        // 锁内第二次检查验证槽位是否被其他活动占用（double-check，确保并发安全）
        validateSlotNotOccupiedByActivity(dto.getSlotTemplateIds(), dto.getActivityDate());

        // 锁内第二次检查 验证槽位是否被订场占用（double-check，确保并发安全）
        validateSlotNotOccupiedByBooking(dto.getSlotTemplateIds(), dto.getActivityDate());

        // 创建活动实体
        VenueActivity activity = VenueActivity.builder()
                .venueId(venueId)
                .courtId(courtId)
                .activityTypeId(dto.getActivityTypeId())
                .activityTypeDesc(activityType.getTypeCode())
                .activityName(dto.getActivityName())
                .imageUrls(dto.getImageUrls())
                .activityDate(dto.getActivityDate())
                .startTime(startTime)
                .endTime(endTime)
                .maxParticipants(dto.getMaxParticipants())
                .status(1)
                .currentParticipants(0)
                .unitPrice(dto.getUnitPrice())
                .description(dto.getDescription())
                .registrationDeadline(dto.getRegistrationDeadline())
                .organizerId(organizerId)
                .organizerType(organizerType.getValue())
                .organizerName(organizerName)
                .merchantBatchId(merchantBatchId)  // 【新增】设置批次ID
                .contactPhone(dto.getContactPhone())
                .minNtrpLevel(dto.getMinNtrpLevel())
                .activityConfig(dto.getActivityConfig())
                .build();

        // 插入活动数据
        venueActivityMapper.insert(activity);
        log.info("活动创建成功 - activityId: {}, batchId: {}",
                activity.getActivityId(), merchantBatchId);

        // 批量创建活动槽位锁定记录
        List<VenueActivitySlotLock> locks = dto.getSlotTemplateIds().stream()
                .map(slotTemplateId -> VenueActivitySlotLock.builder()
                        .activityId(activity.getActivityId())
                        .slotTemplateId(slotTemplateId)
                        .bookingDate(dto.getActivityDate())
                        .build())
                .collect(Collectors.toList());

        // 批量插入锁定记录
        boolean success = activitySlotLockService.saveBatch(locks);
        if (!success) {
            log.error("批量插入活动槽位锁定记录失败 - activityId: {}", activity.getActivityId());
            throw new GloboxApplicationException("活动槽位锁定失败");
        }
        log.info("活动槽位锁定成功 - activityId: {}, 锁定槽位数: {}",
                activity.getActivityId(), locks.size());

        return activity;
    }

    /**
     * 验证槽位是否连续
     */
    private void validateSlotContinuity(List<VenueBookingSlotTemplate> templates) {
        // 按开始时间排序
        List<VenueBookingSlotTemplate> sortedTemplates = templates.stream()
                .sorted(Comparator.comparing(VenueBookingSlotTemplate::getStartTime))
                .toList();

        // 检查每个槽位的结束时间是否等于下一个槽位的开始时间
        for (int i = 0; i < sortedTemplates.size() - 1; i++) {
            LocalTime currentEndTime = sortedTemplates.get(i).getEndTime();
            LocalTime nextStartTime = sortedTemplates.get(i + 1).getStartTime();

            if (!currentEndTime.equals(nextStartTime)) {
                log.warn("槽位不连续 - currentEndTime: {}, nextStartTime: {}",
                        currentEndTime, nextStartTime);
                throw new GloboxApplicationException("活动时段必须连续");
            }
        }

        log.debug("槽位连续性验证通过");
    }

    /**
     * 验证槽位是否被其他活动占用
     */
    private void validateSlotNotOccupiedByActivity(List<Long> slotTemplateIds, LocalDate activityDate) {
        Long count = activitySlotLockMapper.selectCount(
                new LambdaQueryWrapper<VenueActivitySlotLock>()
                        .in(VenueActivitySlotLock::getSlotTemplateId, slotTemplateIds)
                        .eq(VenueActivitySlotLock::getBookingDate, activityDate)
        );

        if (count > 0) {
            log.warn("槽位已被其他活动占用 - 占用数量: {}", count);
            throw new GloboxApplicationException("该时段已被其他活动占用");
        }

        log.debug("槽位未被活动占用验证通过");
    }

    /**
     * 验证槽位是否被订场占用
     */
    private void validateSlotNotOccupiedByBooking(List<Long> slotTemplateIds, LocalDate bookingDate) {
        Long count = slotRecordMapper.selectCount(
                new LambdaQueryWrapper<VenueBookingSlotRecord>()
                        .in(VenueBookingSlotRecord::getSlotTemplateId, slotTemplateIds)
                        .eq(VenueBookingSlotRecord::getBookingDate, bookingDate.atStartOfDay())
                        .in(VenueBookingSlotRecord::getStatus,
                                BookingSlotStatus.LOCKED_IN.getValue(),
                                BookingSlotStatus.EXPIRED.getValue())
        );

        if (count > 0) {
            log.warn("槽位已被订场占用 - 占用数量: {}", count);
            throw new GloboxApplicationException("该时段已被订场占用");
        }

        log.debug("槽位未被订场占用验证通过");
    }

    /**
     * 验证活动日期和时间不能是过去的时间
     */
    private void validateActivityDateTime(LocalDate activityDate, LocalTime startTime) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        if (activityDate.equals(today)) {
            LocalDateTime activityStartDateTime = LocalDateTime.of(activityDate, startTime);
            if (activityStartDateTime.isBefore(now)) {
                log.warn("活动开始时间已过 - activityDate: {}, startTime: {}, now: {}",
                        activityDate, startTime, now);
                throw new GloboxApplicationException("活动开始时间不能是过去的时间");
            }
        }

        log.debug("活动日期时间验证通过 - activityDate: {}, startTime: {}", activityDate, startTime);
    }

    /**
     * 构建分布式锁键
     * 与用户订场使用相同的锁键格式，防止并发冲突
     */
    private String buildLockKey(Long slotTemplateId, LocalDate activityDate) {
        return BookingCacheConstants.BOOKING_LOCK_KEY_PREFIX +
                slotTemplateId +
                BookingCacheConstants.BOOKING_LOCK_KEY_SEPARATOR +
                activityDate;
    }
}