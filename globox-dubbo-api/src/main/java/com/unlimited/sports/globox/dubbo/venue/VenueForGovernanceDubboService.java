package com.unlimited.sports.globox.dubbo.venue;

import com.unlimited.sports.globox.dubbo.governance.dto.ContentSnapshotResultDto;

/**
 * 场馆服务为治理服务提供的 dubbo 接口
 */
public interface VenueForGovernanceDubboService {
    ContentSnapshotResultDto getVenueCommentSnapshot(Long id);
}
