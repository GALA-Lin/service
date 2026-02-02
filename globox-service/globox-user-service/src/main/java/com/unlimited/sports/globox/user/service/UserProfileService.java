package com.unlimited.sports.globox.user.service;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.auth.dto.SetUsernameRequest;
import com.unlimited.sports.globox.model.auth.dto.UpdateStarCardPortraitRequest;
import com.unlimited.sports.globox.model.auth.dto.UpdateUserProfileRequest;
import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import com.unlimited.sports.globox.model.auth.vo.SetUsernameResultVo;
import com.unlimited.sports.globox.model.auth.vo.StarCardPortraitVo;
import com.unlimited.sports.globox.model.auth.vo.StarCardVo;
import com.unlimited.sports.globox.model.auth.vo.ProfileOptionsVo;
import com.unlimited.sports.globox.model.auth.vo.StyleTagVo;
import com.unlimited.sports.globox.model.auth.vo.UserProfileVo;
import com.unlimited.sports.globox.model.auth.vo.UserSearchResultVo;
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
     * 查询用户完整资料（含关注/互关状态）
     *
     * @param userId   目标用户ID
     * @param viewerId 当前用户ID
     * @return 用户资料视图
     */
    R<UserProfileVo> getUserProfile(Long userId, Long viewerId);

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

    /**
     * 获取球星卡肖像
     *
     * @param userId 用户ID
     * @return 球星卡肖像视图
     */
    R<StarCardPortraitVo> getStarCardPortrait(Long userId);

    /**
     * 更新球星卡肖像（支持删除：传 null 或空字符串）
     *
     * @param userId  用户ID
     * @param request 更新请求
     * @return 更新结果
     */
    R<String> updateStarCardPortrait(Long userId, UpdateStarCardPortraitRequest request);

    /**
     * 上传并处理球星卡肖像（自动抠图 - 异步处理）
     *
     * 流程：
     * 1. 接收用户上传的图片
     * 2. 立即返回"审核中"状态
     * 3. 后台异步处理：上传到 COS 并调用数据万象 AI 人像抠图
     * 4. 后台异步更新用户的 portraitUrl 字段
     *
     * @param userId 用户ID
     * @param file   上传的图片文件
     * @return 提示消息
     */
    R<String> uploadStarCardPortrait(Long userId, MultipartFile file);

    /**
     * 设置/修改球盒号（含校验与冷却期检查）
     *
     * 规则：
     * 1. 格式校验：4-20位字母或数字
     * 2. 唯一性校验：username_lower 全局唯一
     * 3. 冷却期校验：距上次修改需满足冷却时间（默认60天）
     * 4. 首次设置时写入 username、username_lower、last_username_changed_at
     *
     * @param userId  用户ID
     * @param request 设置球盒号请求
     * @return 设置结果（包含 username 和 cooldownUntil）
     */
    R<SetUsernameResultVo> setUsername(Long userId, SetUsernameRequest request);

    /**
     * 按球盒号搜索用户（不区分大小写）
     *
     * 搜索规则：
     * 1. 精确匹配优先（LOWER(input) = username_lower）
     * 2. 前缀匹配其次（username_lower LIKE 'input%'）
     * 3. 支持分页
     *
     * @param keyword  搜索关键词
     * @param page     页码（从1开始）
     * @param pageSize 每页大小
     * @return 搜索结果列表
     */
    R<UserSearchResultVo> searchUsersByUsername(String keyword, Integer page, Integer pageSize);
}
