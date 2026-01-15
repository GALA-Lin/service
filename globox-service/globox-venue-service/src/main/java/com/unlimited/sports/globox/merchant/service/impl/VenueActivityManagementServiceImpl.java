package com.unlimited.sports.globox.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.lock.RedisDistributedLock;
import com.unlimited.sports.globox.common.result.VenueCode;
import com.unlimited.sports.globox.cos.vo.BatchUploadResultVo;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.MerchantMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.merchant.service.VenueActivityManagementService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Merchant;
import com.unlimited.sports.globox.model.merchant.entity.VenueStaff;
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
import com.unlimited.sports.globox.venue.service.IFileUploadService;
import com.unlimited.sports.globox.venue.service.IVenueActivitySlotLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
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
    private final RedisDistributedLock redisDistributedLock;
    private final IVenueActivitySlotLockService activitySlotLockService;

    /**
     * 自注入，用于解决@Transactional自调用问题
     * 使用@Lazy避免循环依赖警告
     */
    @Autowired
    @Lazy
    private VenueActivityManagementServiceImpl self;

    @Autowired
    private IFileUploadService fileUploadService;

    @Override
    public Long createActivity(CreateActivityDto dto, MultipartFile[] images, MerchantAuthContext context) {        log.info("创建活动 - employeeId: {}, role: {}, activityName: {}, slotTemplateIds: {}",
                context.getEmployeeId(), context.getRole(), dto.getActivityName(), dto.getSlotTemplateIds());

        // 1. 处理可选的图片上传（在事务之外处理网络请求）
        if (images != null && images.length > 0) {
            log.info("开始上传活动图片，数量：{}", images.length);
            // 使用 FileTypeEnum.VENUE_IMAGE 或新增 ACTIVITY_IMAGE 类型
            BatchUploadResultVo uploadResult = fileUploadService.batchUploadFiles(images, FileTypeEnum.VENUE_IMAGE);

            if (uploadResult.getSuccessCount() > 0) {
                // 将上传成功的 URL 设置到 DTO 中
                dto.setImageUrls(uploadResult.getSuccessUrls());
            }
        }

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
            organizerName = staff.getDisplayName() != null ? staff.getDisplayName() : "未知员工";
        }

        // 查询槽位模板
        List<VenueBookingSlotTemplate> templates = slotTemplateMapper.selectBatchIds(dto.getSlotTemplateIds());
        if (templates.size() != dto.getSlotTemplateIds().size()) {
            log.warn("部分槽位模板不存在 - 请求{}个，找到{}个", dto.getSlotTemplateIds().size(), templates.size());
            throw new GloboxApplicationException(VenueCode.SLOT_TEMPLATE_NOT_EXIST);
        }

        //验证槽位是否来自同一场地
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

        //提取开始时间和结束时间
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

        // 验证槽位是否被其他活动占用（快速失败，减少锁竞争）
        validateSlotNotOccupiedByActivity(dto.getSlotTemplateIds(), dto.getActivityDate());

        // 验证槽位是否被订场占用（快速失败，减少锁竞争）
        validateSlotNotOccupiedByBooking(dto.getSlotTemplateIds(), dto.getActivityDate());

        //构建分布式锁键列表（与用户订场使用相同的锁键格式）
        List<String> lockKeys = dto.getSlotTemplateIds().stream()
                .map(slotId -> buildLockKey(slotId, dto.getActivityDate()))
                .collect(Collectors.toList());

        List<RLock> locks = null;

        try {
            //批量获取所有槽位的分布式锁
            locks = redisDistributedLock.tryLockMultiple(lockKeys, 1, -1L, TimeUnit.SECONDS);
            if (locks == null) {
                throw new GloboxApplicationException("该时段正在被操作，请稍后重试");
            }
            log.info("成功获取分布式锁 - organizerId: {}, organizerType: {}, 槽位数: {}",
                    organizerId, organizerType, dto.getSlotTemplateIds().size());

            // 在事务内执行活动创建（通过self调用以确保事务生效）
            VenueActivity activity = self.createActivityWithinTransaction(
                    dto, organizerId, organizerType, organizerName,
                    venueId, courtId, activityType, startTime, endTime
            );

            log.info("活动创建成功 - activityId: {}, organizerId: {}, organizerName: {}",
                    activity.getActivityId(), organizerId, organizerName);

            return activity.getActivityId();

        } finally {
            //  释放锁（无论事务成功还是失败）
            if (locks != null) {
                redisDistributedLock.unlockMultiple(locks);
                log.info("【锁包事务】释放分布式锁 - organizerId: {}, organizerType: {}", organizerId, organizerType);
            }
        }
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
            LocalTime endTime) {

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
                .activityDate(dto.getActivityDate())
                .startTime(startTime)
                .endTime(endTime)
                .maxParticipants(dto.getMaxParticipants())
                .currentParticipants(0)
                .unitPrice(dto.getUnitPrice())
                .description(dto.getDescription())
                .registrationDeadline(dto.getRegistrationDeadline())
                .organizerId(organizerId)
                .organizerType(organizerType.getValue())
                .organizerName(organizerName)
                .contactPhone(dto.getContactPhone())
                .minNtrpLevel(dto.getMinNtrpLevel())
                .activityConfig(dto.getActivityConfig())
                .status(VenueActivityStatusEnum.NORMAL.getValue())
                .imageUrls(dto.getImageUrls() != null ? Collections.singletonList(String.join(",", dto.getImageUrls())) : null) // 示例：转为逗号分隔字符串
                .build();

        // 插入活动数据
        venueActivityMapper.insert(activity);
        log.info("活动创建成功 - activityId: {}", activity.getActivityId());

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
        log.info("活动槽位锁定成功 - activityId: {}, 锁定槽位数: {}", activity.getActivityId(), locks.size());

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
    private void validateSlotNotOccupiedByActivity(List<Long> slotTemplateIds, java.time.LocalDate activityDate) {
        // 查询这些槽位在指定日期是否已被其他活动占用
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
    private void validateSlotNotOccupiedByBooking(List<Long> slotTemplateIds, java.time.LocalDate bookingDate) {
        // 查询这些槽位在指定日期是否已被订场占用
        // 状态为LOCKED_IN(2)或EXPIRED(3)才算占用
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
     *
     * @param activityDate 活动日期
     * @param startTime 活动开始时间
     */
    private void validateActivityDateTime(LocalDate activityDate, LocalTime startTime) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 如果活动日期是今天，检查活动开始时间是否已经过去
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
     *
     * @param slotTemplateId 槽位模板ID
     * @param activityDate 活动日期
     * @return 锁键
     */
    private String buildLockKey(Long slotTemplateId, java.time.LocalDate activityDate) {
        return BookingCacheConstants.BOOKING_LOCK_KEY_PREFIX + slotTemplateId + BookingCacheConstants.BOOKING_LOCK_KEY_SEPARATOR + activityDate;
    }
}
