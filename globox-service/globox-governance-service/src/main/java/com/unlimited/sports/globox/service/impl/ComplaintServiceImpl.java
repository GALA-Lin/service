package com.unlimited.sports.globox.service.impl;

import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.enums.governance.ComplaintReasonTypeEnum;
import com.unlimited.sports.globox.common.enums.governance.ComplaintStatusEnum;
import com.unlimited.sports.globox.common.enums.governance.ComplaintTargetTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.GovernanceCode;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.dubbo.governance.dto.ContentSnapshotResultDto;
import com.unlimited.sports.globox.dubbo.social.SocialForGovernanceDubboService;
import com.unlimited.sports.globox.dubbo.user.UserForGovernanceDubboService;
import com.unlimited.sports.globox.dubbo.venue.VenueForGovernanceDubboService;
import com.unlimited.sports.globox.mapper.ComplaintEvidencesMapper;
import com.unlimited.sports.globox.mapper.ComplaintSnapshotsMapper;
import com.unlimited.sports.globox.mapper.ComplaintMapper;
import com.unlimited.sports.globox.model.governance.dto.CreateComplaintRequestDto;
import com.unlimited.sports.globox.model.governance.entity.ComplaintEvidences;
import com.unlimited.sports.globox.model.governance.entity.ComplaintSnapshots;
import com.unlimited.sports.globox.model.governance.entity.Complaints;
import com.unlimited.sports.globox.service.ComplaintService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 举报工单 服务层
 */
@Slf4j
@Service
public class ComplaintServiceImpl implements ComplaintService {

    @Autowired
    private ComplaintMapper complaintMapper;

    @Autowired
    private ComplaintSnapshotsMapper complaintSnapshotsMapper;

    @Autowired
    private ComplaintEvidencesMapper complaintEvidencesMapper;

    @DubboReference(group = "rpc")
    private SocialForGovernanceDubboService socialForGovernanceDubboService;

    @DubboReference(group = "rpc")
    private UserForGovernanceDubboService userForGovernanceDubboService;

    @DubboReference(group = "rpc")
    private VenueForGovernanceDubboService venueForGovernanceDubboService;

    @Autowired
    private JsonUtils jsonUtils;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createComplaint(CreateComplaintRequestDto dto, Long userId, ClientType clientType) {
        // 1) 基础校验
        if (!ComplaintTargetTypeEnum.isValid(dto.getTargetType())) {
            throw new GloboxApplicationException(GovernanceCode.COMPLAINT_TARGET_TYPE_INVALID);
        }
        if (!ComplaintReasonTypeEnum.isValid(dto.getReason())) {
            throw new GloboxApplicationException(GovernanceCode.COMPLAINT_REASON_INVALID);
        }

        Assert.isTrue(!userId.equals(dto.getTargetUserId()), GovernanceCode.CANT_COMPLAINT_SELF);

        List<String> urls = dto.getEvidenceUrls();

        // 3) 保存 reports
        Complaints report = Complaints.builder()
                .userId(userId)
                .clientType(clientType)
                .targetType(ComplaintTargetTypeEnum.of(dto.getTargetType()))
                .targetId(dto.getTargetId())
                .targetUserId(dto.getTargetUserId())
                .reason(ComplaintReasonTypeEnum.of(dto.getReason()))
                .description(dto.getDescription())
                .status(ComplaintStatusEnum.PENDING)
                .handledAt(null)
                .build();


        int cnt = complaintMapper.insert(report);
        if (cnt <= 0) {
            throw new GloboxApplicationException(GovernanceCode.COMPLAINT_CREATE_FAILED);
        }

        // 4) 保存快照 report_snapshots（一单一快照）
        ComplaintSnapshots snapshot = buildSnapshot(dto, report.getId());

        cnt = complaintSnapshotsMapper.insert(snapshot);
        if (cnt <= 0) {
            throw new GloboxApplicationException(GovernanceCode.COMPLAINT_SNAPSHOT_CREATE_FAILED);
        }

        // 5) 保存证据 report_evidences（最多9张）
        if (urls != null && !urls.isEmpty()) {

            // 过滤空字符串，保证 seq 连续
            List<String> cleaned = urls.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            if (!cleaned.isEmpty()) {
                for (int i = 0; i < cleaned.size(); i++) {
                    ComplaintEvidences e = new ComplaintEvidences();
                    e.setComplaintId(report.getId());
                    e.setSeq(i + 1);
                    e.setUrl(cleaned.get(i));
                    cnt = complaintEvidencesMapper.insert(e);
                    if (cnt <= 0) {
                        throw new GloboxApplicationException(GovernanceCode.COMPLAINT_EVIDENCE_CREATE_FAILED);
                    }
                }
            }
        }
    }


    private ComplaintSnapshots buildSnapshot(CreateComplaintRequestDto dto, Long id) {
        ComplaintTargetTypeEnum targetType = ComplaintTargetTypeEnum.of(dto.getTargetType());
        ContentSnapshotResultDto resultDto = switch (targetType) {
            // 帖子
            case NOTE -> socialForGovernanceDubboService.getNoteSnapshot(id);
            // 帖子评论
            case NOTE_COMMENT -> socialForGovernanceDubboService.getNoteCommentSnapshot(id);
            // 聊天
            case IM_MESSAGE -> socialForGovernanceDubboService.getIMMessageSnapshot(id);
            // 用户信息
            case USER_PROFILE -> userForGovernanceDubboService.getUserProfileSnapshot(id);
            // 场馆评论
            case VENUE_COMMENT -> venueForGovernanceDubboService.getVenueCommentSnapshot(id);
        };


        String contentText = resultDto.getContent();
        String contentJson = jsonUtils.objectToJson(resultDto);

        return ComplaintSnapshots.builder()
                .complaintId(id)
                .targetType(targetType)
                .targetId(dto.getTargetId())
                .targetUserId(dto.getTargetUserId())
                .contentText(contentText)
                .contentJson(contentJson)
                .build();
    }
}
