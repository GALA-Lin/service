package com.unlimited.sports.globox.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.model.auth.dto.UpdateUserMediaRequest;
import com.unlimited.sports.globox.model.auth.dto.UserMediaRequest;
import com.unlimited.sports.globox.model.auth.entity.UserMedia;
import com.unlimited.sports.globox.model.auth.vo.UserMediaVo;
import com.unlimited.sports.globox.model.venue.vo.FileUploadVo;
import com.unlimited.sports.globox.user.mapper.UserMediaMapper;
import com.unlimited.sports.globox.user.service.FileUploadService;
import com.unlimited.sports.globox.user.service.UserMediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户媒体服务实现类
 *
 * @author Wreckloud
 * @since 2025/12/26
 */
@Service
@Slf4j
public class UserMediaServiceImpl implements UserMediaService {

    private static final int MAX_MEDIA_COUNT = 12; // 最大媒体数量（图+视频合计）
    private static final int MAX_VIDEO_COUNT = 5;  // 最大视频数量

    @Autowired
    private UserMediaMapper userMediaMapper;

    @Autowired
    private FileUploadService fileUploadService;

    @Override
    public R<List<UserMediaVo>> getUserMediaList(Long userId, String mediaType) {
        if (userId == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }

        // 1. 构建查询条件
        LambdaQueryWrapper<UserMedia> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserMedia::getUserId, userId)
                .eq(UserMedia::getStatus, UserMedia.Status.ACTIVE);

        // 2. 如果指定了媒体类型，添加过滤条件
        if (StringUtils.hasText(mediaType)) {
            try {
                UserMedia.MediaType type = UserMedia.MediaType.valueOf(mediaType.toUpperCase());
                queryWrapper.eq(UserMedia::getMediaType, type);
            } catch (IllegalArgumentException e) {
                return R.error(UserAuthCode.INVALID_PARAM);
            }
        }

        // 3. 按排序字段升序排序
        queryWrapper.orderByAsc(UserMedia::getSort);

        // 4. 查询媒体列表
        List<UserMedia> mediaList = userMediaMapper.selectList(queryWrapper);

        // 5. 转换为VO
        List<UserMediaVo> voList = mediaList.stream()
                .map(media -> {
                    UserMediaVo vo = new UserMediaVo();
                    BeanUtils.copyProperties(media, vo);
                    vo.setMediaType(media.getMediaType() != null ? media.getMediaType().name() : null);
                    return vo;
                })
                .collect(Collectors.toList());

        return R.ok(voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateUserMedia(Long userId, UpdateUserMediaRequest request) {
        if (userId == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }

        List<UserMediaRequest> mediaList = request.getMediaList();

        // 1. 空列表 = 清空所有媒体
        if (CollectionUtils.isEmpty(mediaList)) {
            LambdaQueryWrapper<UserMedia> deleteQuery = new LambdaQueryWrapper<>();
            deleteQuery.eq(UserMedia::getUserId, userId);
            userMediaMapper.delete(deleteQuery);
            log.info("用户媒体列表已清空：userId={}", userId);
            return R.ok("媒体列表已清空");
        }

        // 2. 校验媒体类型和数量限制
        int videoCount = 0;
        Set<Integer> sortSet = new HashSet<>();

        for (UserMediaRequest mediaReq : mediaList) {
            // 2.1 校验媒体类型
            if (!StringUtils.hasText(mediaReq.getMediaType())) {
                throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
            }

            try {
                UserMedia.MediaType type = UserMedia.MediaType.valueOf(mediaReq.getMediaType().toUpperCase());
                if (type == UserMedia.MediaType.VIDEO) {
                    videoCount++;
                    if (!StringUtils.hasText(mediaReq.getCoverUrl())) {
                        throw new GloboxApplicationException(UserAuthCode.VIDEO_COVER_REQUIRED);
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
            }

            if (!StringUtils.hasText(mediaReq.getUrl())) {
                throw new GloboxApplicationException(UserAuthCode.MEDIA_URL_REQUIRED);
            }

            // 2.2 校验排序值不能重复
            int sortValue = mediaReq.getSort() != null ? mediaReq.getSort() : 0;
            if (sortSet.contains(sortValue)) {
                throw new GloboxApplicationException(UserAuthCode.MEDIA_SORT_DUPLICATE);
            }
            sortSet.add(sortValue);
        }

        // 2.3 校验数量限制
        if (mediaList.size() > MAX_MEDIA_COUNT) {
            throw new GloboxApplicationException(UserAuthCode.MEDIA_COUNT_EXCEEDED);
        }
        if (videoCount > MAX_VIDEO_COUNT) {
            throw new GloboxApplicationException(UserAuthCode.VIDEO_COUNT_EXCEEDED);
        }

        // 3. 全量替换：先删除用户所有现有媒体记录
        LambdaQueryWrapper<UserMedia> deleteQuery = new LambdaQueryWrapper<>();
        deleteQuery.eq(UserMedia::getUserId, userId);
        userMediaMapper.delete(deleteQuery);

        // 4. 批量插入新记录
        for (UserMediaRequest mediaReq : mediaList) {
            UserMedia userMedia = new UserMedia();
            userMedia.setUserId(userId);
            userMedia.setMediaType(UserMedia.MediaType.valueOf(mediaReq.getMediaType().toUpperCase()));
            userMedia.setUrl(mediaReq.getUrl());
            userMedia.setCoverUrl(mediaReq.getCoverUrl());
            userMedia.setDuration(mediaReq.getDuration());
            userMedia.setSize(mediaReq.getSize());
            userMedia.setSort(mediaReq.getSort() != null ? mediaReq.getSort() : 0);
            userMedia.setStatus(UserMedia.Status.ACTIVE);
            userMediaMapper.insert(userMedia);
        }

        log.info("用户媒体列表更新成功：userId={}, count={}", userId, mediaList.size());
        return R.ok("媒体列表更新成功");
    }

    @Override
    public R<FileUploadVo> uploadMediaImage(MultipartFile file) {
        try {
            // 调用文件上传服务上传图片
            String fileUrl = fileUploadService.uploadFile(file, FileTypeEnum.USER_MEDIA_IMAGE);

            // 构建返回结果
            FileUploadVo vo = new FileUploadVo(
                    fileUrl,
                    file.getOriginalFilename(),
                    file.getSize()
            );

            log.info("媒体图片上传成功: fileUrl={}", fileUrl);
            return R.ok(vo);
        } catch (GloboxApplicationException e) {
            log.error("媒体图片上传失败: {}", e.getMessage());
            return R.error(e);
        } catch (Exception e) {
            log.error("媒体图片上传异常", e);
            return R.<FileUploadVo>error(UserAuthCode.UPLOAD_FILE_FAILED).message("图片上传失败");
        }
    }

    @Override
    public R<FileUploadVo> uploadMediaVideo(MultipartFile file) {
        try {
            // 调用文件上传服务上传视频
            String fileUrl = fileUploadService.uploadFile(file, FileTypeEnum.USER_MEDIA_VIDEO);

            // 构建返回结果
            FileUploadVo vo = new FileUploadVo(
                    fileUrl,
                    file.getOriginalFilename(),
                    file.getSize()
            );

            log.info("媒体视频上传成功: fileUrl={}", fileUrl);
            return R.ok(vo);
        } catch (GloboxApplicationException e) {
            log.error("媒体视频上传失败: {}", e.getMessage());
            return R.error(e);
        } catch (Exception e) {
            log.error("媒体视频上传异常", e);
            return R.<FileUploadVo>error(UserAuthCode.UPLOAD_FILE_FAILED).message("视频上传失败");
        }
    }
}

