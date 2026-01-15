package com.unlimited.sports.globox.user.service;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.auth.dto.UpdateUserMediaRequest;
import com.unlimited.sports.globox.model.auth.vo.UserMediaVo;
import com.unlimited.sports.globox.model.venue.vo.FileUploadVo;
import com.unlimited.sports.globox.user.vo.VideoUploadVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户媒体服务接口
 *
 * @author Wreckloud
 * @since 2025/12/26
 */
public interface UserMediaService {

    /**
     * 查询用户媒体列表
     *
     * @param userId    用户ID
     * @param mediaType 媒体类型（可选，IMAGE/VIDEO，为空则返回所有类型）
     * @return 媒体列表
     */
    R<List<UserMediaVo>> getUserMediaList(Long userId, String mediaType);

    /**
     * 保存用户媒体列表（全量替换）
     *
     * @param userId  用户ID
     * @param request 更新请求
     * @return 保存结果
     */
    R<String> updateUserMedia(Long userId, UpdateUserMediaRequest request);

    /**
     * 上传媒体图片
     *
     * @param file 图片文件
     * @return 上传结果
     */
    R<FileUploadVo> uploadMediaImage(MultipartFile file);

    /**
     * 上传媒体视频（自动生成封面）
     *
     * @param file 视频文件
     * @return 上传结果（包含封面URL）
     */
    R<VideoUploadVo> uploadMediaVideo(MultipartFile file);
}

