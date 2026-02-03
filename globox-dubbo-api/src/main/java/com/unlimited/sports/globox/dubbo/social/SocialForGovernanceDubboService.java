package com.unlimited.sports.globox.dubbo.social;

import com.unlimited.sports.globox.dubbo.governance.dto.ContentSnapshotResultDto;

/**
 * 社交服务对治理服务提供的 dubbo 接口
 */
public interface SocialForGovernanceDubboService {
    ContentSnapshotResultDto getNoteSnapshot(Long id);

    ContentSnapshotResultDto getNoteCommentSnapshot(Long id);

    ContentSnapshotResultDto getIMMessageSnapshot(Long id);


}
