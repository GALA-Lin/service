package com.unlimited.sports.globox.user.dubbo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.dubbo.governance.dto.ContentSnapshotResultDto;
import com.unlimited.sports.globox.dubbo.user.UserForGovernanceDubboService;
import com.unlimited.sports.globox.model.auth.entity.UserMedia;
import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import com.unlimited.sports.globox.user.mapper.UserMediaMapper;
import com.unlimited.sports.globox.user.mapper.UserProfileMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户服务为治理服务提供的 dubbo 接口
 */
@Component
@DubboService(group = "rpc")
public class UserForGovernanceDubboServiceImpl implements UserForGovernanceDubboService {

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private UserMediaMapper userMediaMapper;

    @Autowired
    private JsonUtils jsonUtils;

    @Override
    public ContentSnapshotResultDto getUserProfileSnapshot(Long id) {
        UserProfile userProfile = userProfileMapper.selectOne(
                Wrappers.<UserProfile>lambdaQuery()
                        .eq(UserProfile::getUserId, id));

        List<UserMedia> userMedia = userMediaMapper.selectList(
                Wrappers.<UserMedia>lambdaQuery()
                        .eq(UserMedia::getUserId, id));

        List<String> mediaList = userMedia.stream().map(UserMedia::getUrl).toList();

        String userProfileJson = jsonUtils.objectToJson(userProfile);

        return ContentSnapshotResultDto.builder()
                .id(id)
                .content(userProfileJson)
                .mediaList(mediaList)
                .build();
    }
}
