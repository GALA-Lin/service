package com.unlimited.sports.globox.dubbo.user;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.model.auth.vo.UserSyncVo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户搜索数据RPC服务接口
 * 提供用户数据增量同步功能
 */
public interface IUserSearchDataService {

    /**
     * 增量同步用户数据
     *
     * @param updatedTime 上一次同步的时间戳，为空表示同步全部数据，不为空表示同步该时间之后的数据
     * @return 同步的用户数据列表（UserSyncVo格式）
     */
    RpcResult<List<UserSyncVo>> syncUserData(LocalDateTime updatedTime);
}
