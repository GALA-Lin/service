package com.unlimited.sports.globox.user.service;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.auth.dto.UpdateUserProfileRequest;
import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import com.unlimited.sports.globox.model.auth.vo.StarCardVo;
import com.unlimited.sports.globox.model.auth.vo.ProfileOptionsVo;
import com.unlimited.sports.globox.model.auth.vo.StyleTagVo;
import com.unlimited.sports.globox.model.auth.vo.UserProfileVo;
import com.unlimited.sports.globox.model.venue.vo.FileUploadVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户资料服务接口
 *
 * @author Wreckloud
 * @since 2025/12/20
 */
public interface UserProfileService {

    /**
     * 根据用户ID获取用户资料（内部使用）
     *
     * @param userId 用户ID
     * @return 用户资料，不存在返回null
     */
    UserProfile getUserProfileById(Long userId);

    /**
     * 批量获取用户资料（内部使用）
     *
     * @param userIds 用户ID列表（最多50个）
     * @return 用户资料列表（不存在的用户会被过滤）
     */
    List<UserProfile> batchGetUserProfile(List<Long> userIds);

    /**
     * 查询用户完整资料（含球拍+标签）
     *
     * @param userId 用户ID
     * @return 用户资料视图
     */
    R<UserProfileVo> getUserProfile(Long userId);

    /**
     * 更新用户资料（patch更新，只更新非空字段）
     *
     * @param userId  用户ID
     * @param request 更新请求
     * @return 更新结果
     */
    R<String> updateUserProfile(Long userId, UpdateUserProfileRequest request);

    /**
     * 获取资料可选项（球拍字典和球风标签，仅返回ACTIVE状态）
     *
     * @return 资料可选项视图
     */
    R<ProfileOptionsVo> getProfileOptions();

    /**
     * 获取用户球星卡数据
     *
     * @param userId 用户ID
     * @return 球星卡视图
     */
    R<StarCardVo> getStarCard(Long userId);

    /**
     * 获取所有球风标签（仅返回ACTIVE状态）
     *
     * @return 球风标签列表
     */
    R<List<StyleTagVo>> getStyleTags();

    /**
     * 上传用户头像
     *
     * @param file 头像文件
     * @return 文件上传结果（包含URL、文件名、文件大小）
     */
    R<FileUploadVo> uploadAvatar(MultipartFile file);
}
