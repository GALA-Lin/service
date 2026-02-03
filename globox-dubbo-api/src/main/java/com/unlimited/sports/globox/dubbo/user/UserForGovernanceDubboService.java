package com.unlimited.sports.globox.dubbo.user;

import com.unlimited.sports.globox.dubbo.governance.dto.ContentSnapshotResultDto;

/**
 * 用户服务为治理服务提供的 dubbo 接口
 */
public interface UserForGovernanceDubboService {
    ContentSnapshotResultDto getUserProfileSnapshot(Long id);
}
