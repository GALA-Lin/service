package com.unlimited.sports.globox.service.impl;

import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.enums.governance.ComplaintReasonTypeEnum;
import com.unlimited.sports.globox.common.enums.governance.ComplaintStatusEnum;
import com.unlimited.sports.globox.common.enums.governance.ComplaintTargetTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.GovernanceCode;
import com.unlimited.sports.globox.mapper.ComplaintEvidencesMapper;
import com.unlimited.sports.globox.mapper.ReportSnapshotsMapper;
import com.unlimited.sports.globox.mapper.ComplaintMapper;
import com.unlimited.sports.globox.model.governance.dto.CreateComplaintRequestDto;
import com.unlimited.sports.globox.model.governance.entity.ComplaintEvidences;
import com.unlimited.sports.globox.model.governance.entity.ComplaintSnapshots;
import com.unlimited.sports.globox.model.governance.entity.Complaints;
import com.unlimited.sports.globox.service.ComplaintService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private ReportSnapshotsMapper reportSnapshotsMapper;

    @Autowired
    private ComplaintEvidencesMapper complaintEvidencesMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createComplaint(CreateComplaintRequestDto dto, Long userId, ClientType clientType) {
        // 1) 基础校验（枚举合法性建议你在 DTO 校验或这里做）
        if (!ComplaintTargetTypeEnum.isValid(dto.getTargetType())) {
            throw new GloboxApplicationException(GovernanceCode.REPORT_TARGET_TYPE_INVALID);
        }
        if (!ComplaintReasonTypeEnum.isValid(dto.getReason())) {
            throw new GloboxApplicationException(GovernanceCode.REPORT_REASON_INVALID);
        }

        List<String> urls = dto.getEvidenceUrls();
        LocalDateTime now = LocalDateTime.now();

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
            throw new GloboxApplicationException(GovernanceCode.REPORT_CREATE_FAILED);
        }

        // 4) 保存快照 report_snapshots（一单一快照）
        ComplaintSnapshots snapshot = buildSnapshot(dto, report.getId(), now);

        cnt = reportSnapshotsMapper.insert(snapshot);
        if (cnt <= 0) {
            throw new GloboxApplicationException(GovernanceCode.REPORT_SNAPSHOT_CREATE_FAILED);
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
                        throw new GloboxApplicationException(GovernanceCode.REPORT_EVIDENCE_CREATE_FAILED);
                    }
                }
            }
        }
    }

    private ComplaintSnapshots buildSnapshot(CreateComplaintRequestDto dto, Long id, LocalDateTime now) {
        ComplaintTargetTypeEnum targetType = ComplaintTargetTypeEnum.of(dto.getTargetType());
        switch (targetType) {
            // 帖子
            case NOTE -> {
            }
            // 帖子评论
            case NOTE_COMMENT -> {
            }
            // 聊天
            case IM_MESSAGE -> {
            }
            // 用户信息
            case USER_PROFILE -> {
            }
            // 场馆评论
            case VENUE_COMMENT -> {
            }
        }


        // 构建 contentText
        // TODO 各个服务需要提供接口，供此处查询
        String contentText = "to be update";
        // 构建 contentJson
        String contentJson = "{}";

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
