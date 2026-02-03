package com.unlimited.sports.globox.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qcloud.cos.COSClient;
import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.prop.CosProperties;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.common.utils.FilePathUtil;
import com.unlimited.sports.globox.common.utils.FileValidationUtil;
import com.unlimited.sports.globox.cos.CosFileUploadUtil;
import com.unlimited.sports.globox.dubbo.social.SocialRelationDubboService;
import com.unlimited.sports.globox.model.auth.dto.UpdateUserMediaRequest;
import com.unlimited.sports.globox.model.auth.dto.UserMediaRequest;
import com.unlimited.sports.globox.model.auth.entity.UserMedia;
import com.unlimited.sports.globox.model.auth.vo.UserMediaVo;
import com.unlimited.sports.globox.model.venue.vo.FileUploadVo;
import com.unlimited.sports.globox.user.mapper.UserMediaMapper;
import com.unlimited.sports.globox.user.service.FileUploadService;
import com.unlimited.sports.globox.user.service.UserMediaService;
import com.unlimited.sports.globox.user.vo.VideoUploadVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

    @Autowired
    private COSClient cosClient;

    @Autowired
    private CosProperties cosProperties;

    @DubboReference(group = "rpc")
    private SocialRelationDubboService socialRelationDubboService;

    @Override
    // 查询自己的媒体列表
    public R<List<UserMediaVo>> getUserMediaList(Long userId, String mediaType) {
        if (userId == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }

        LambdaQueryWrapper<UserMedia> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserMedia::getUserId, userId)
                .eq(UserMedia::getStatus, UserMedia.Status.ACTIVE)
                .eq(UserMedia::getDeleted, 0);

        if (StringUtils.hasText(mediaType)) {
            try {
                UserMedia.MediaType type = UserMedia.MediaType.valueOf(mediaType.toUpperCase());
                queryWrapper.eq(UserMedia::getMediaType, type);
            } catch (IllegalArgumentException e) {
                return R.error(UserAuthCode.INVALID_PARAM);
            }
        }

        queryWrapper.orderByAsc(UserMedia::getSort);

        List<UserMedia> mediaList = userMediaMapper.selectList(queryWrapper);

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
    // 查询他人媒体列表（含拉黑校验）
    public R<List<UserMediaVo>> getUserMediaList(Long targetUserId, String mediaType, Long viewerId) {
        if (targetUserId == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }

        if (viewerId != null && !viewerId.equals(targetUserId)) {
            try {
                RpcResult<Boolean> blockedResult = socialRelationDubboService.isBlocked(viewerId, targetUserId);
                if (blockedResult != null && blockedResult.isSuccess() && Boolean.TRUE.equals(blockedResult.getData())) {
                    log.warn("用户{}尝试查看被拉黑用户{}的媒体列表", viewerId, targetUserId);
                    return R.error(SocialCode.USER_BLOCKED);
                }
            } catch (Exception e) {
                log.error("检查拉黑关系失败 viewerId={}, targetUserId={}", viewerId, targetUserId, e);
            }
        }

        LambdaQueryWrapper<UserMedia> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserMedia::getUserId, targetUserId)
                .eq(UserMedia::getStatus, UserMedia.Status.ACTIVE)
                .eq(UserMedia::getDeleted, 0);

        if (StringUtils.hasText(mediaType)) {
            try {
                UserMedia.MediaType type = UserMedia.MediaType.valueOf(mediaType.toUpperCase());
                queryWrapper.eq(UserMedia::getMediaType, type);
            } catch (IllegalArgumentException e) {
                return R.error(UserAuthCode.INVALID_PARAM);
            }
        }

        queryWrapper.orderByAsc(UserMedia::getSort);

        List<UserMedia> mediaList = userMediaMapper.selectList(queryWrapper);

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
    // 保存用户媒体列表（全量替换）
    public R<String> updateUserMedia(Long userId, UpdateUserMediaRequest request) {
        if (userId == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }

        List<UserMediaRequest> mediaList = request.getMediaList();

        if (CollectionUtils.isEmpty(mediaList)) {
            LambdaQueryWrapper<UserMedia> deleteQuery = new LambdaQueryWrapper<>();
            deleteQuery.eq(UserMedia::getUserId, userId);
            UserMedia updateEntity = new UserMedia();
            updateEntity.setDeleted(1);
            updateEntity.setStatus(UserMedia.Status.DISABLED);
            userMediaMapper.update(updateEntity, deleteQuery);
            log.info("用户媒体列表已清空：userId={}", userId);
            return R.ok("媒体列表已清空");
        }

        int videoCount = 0;
        Set<Integer> sortSet = new HashSet<>();

        for (UserMediaRequest mediaReq : mediaList) {
            if (!StringUtils.hasText(mediaReq.getMediaType())) {
                throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
            }

            if (!StringUtils.hasText(mediaReq.getUrl())) {
                throw new GloboxApplicationException(UserAuthCode.MEDIA_URL_REQUIRED);
            }

            try {
                UserMedia.MediaType type = UserMedia.MediaType.valueOf(mediaReq.getMediaType().toUpperCase());
                if (type == UserMedia.MediaType.VIDEO) {
                    videoCount++;
                    if (!StringUtils.hasText(mediaReq.getCoverUrl()) || isVideoCover(mediaReq.getCoverUrl(), mediaReq.getUrl())) {
                        mediaReq.setCoverUrl(buildSnapshotUrl(mediaReq.getUrl()));
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
            }

            int sortValue = mediaReq.getSort() != null ? mediaReq.getSort() : 0;
            if (sortSet.contains(sortValue)) {
                throw new GloboxApplicationException(UserAuthCode.MEDIA_SORT_DUPLICATE);
            }
            sortSet.add(sortValue);
        }

        if (mediaList.size() > MAX_MEDIA_COUNT) {
            throw new GloboxApplicationException(UserAuthCode.MEDIA_COUNT_EXCEEDED);
        }
        if (videoCount > MAX_VIDEO_COUNT) {
            throw new GloboxApplicationException(UserAuthCode.VIDEO_COUNT_EXCEEDED);
        }

        LambdaQueryWrapper<UserMedia> deleteQuery = new LambdaQueryWrapper<>();
        deleteQuery.eq(UserMedia::getUserId, userId);
        UserMedia updateEntity = new UserMedia();
        updateEntity.setDeleted(1);
        updateEntity.setStatus(UserMedia.Status.DISABLED);
        userMediaMapper.update(updateEntity, deleteQuery);

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
            userMedia.setDeleted(0);
            userMediaMapper.insert(userMedia);
        }

        log.info("用户媒体列表更新成功：userId={}, count={}", userId, mediaList.size());
        return R.ok("媒体列表更新成功");
    }

    private static boolean isVideoCover(String coverUrl, String videoUrl) {
        if (!StringUtils.hasText(coverUrl)) {
            return false;
        }
        if (StringUtils.hasText(videoUrl) && coverUrl.equals(videoUrl)) {
            return true;
        }
        String lower = coverUrl.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".m4v");
    }

    private static String buildSnapshotUrl(String fileUrl) {
        String separator = fileUrl.contains("?") ? "&" : "?";
        return fileUrl + separator + "ci-process=snapshot&time=0&format=jpg";
    }

    @Override
    // 上传媒体图片
    public R<FileUploadVo> uploadMediaImage(MultipartFile file) {
        try {
            String fileUrl = fileUploadService.uploadFile(file, FileTypeEnum.USER_MEDIA_IMAGE);

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
    // 上传媒体视频
    public R<VideoUploadVo> uploadMediaVideo(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new GloboxApplicationException(UserAuthCode.MISSING_UPLOAD_FILE);
            }

            String originalFileName = file.getOriginalFilename();
            if (!StringUtils.hasText(originalFileName)) {
                throw new GloboxApplicationException(UserAuthCode.MISSING_UPLOAD_FILE);
            }

            long fileSize = file.getSize();
            if (fileSize <= 0) {
                throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
            }

            FileTypeEnum fileType = FileTypeEnum.USER_MEDIA_VIDEO;
            if (fileSize > fileType.getDefaultMaxSize()) {
                throw new GloboxApplicationException(UserAuthCode.UPLOAD_FILE_TOO_LARGE);
            }

            String extension = FileValidationUtil.getFileExtension(originalFileName);
            if (!fileType.isExtensionAllowed(extension)) {
                throw new GloboxApplicationException(UserAuthCode.UPLOAD_FILE_TYPE_NOT_SUPPORTED);
            }

            String filePath = FilePathUtil.generateFilePath(
                    originalFileName,
                    fileType,
                    cosProperties.getPathPrefix()
            );

            String coverUrl = CosFileUploadUtil.uploadVideoWithCover(
                    cosClient,
                    cosProperties,
                    file,
                    fileType,
                    null,
                    filePath
            );

            String fileUrl = FilePathUtil.buildFileUrl(
                    filePath,
                    cosProperties.getDomain(),
                    cosProperties.getBucketName(),
                    cosProperties.getRegion()
            );

            VideoUploadVo vo = new VideoUploadVo(
                    fileUrl,
                    file.getOriginalFilename(),
                    file.getSize(),
                    coverUrl
            );

            log.info("媒体视频上传成功: fileUrl={}, coverUrl={}", fileUrl, coverUrl);
            return R.ok(vo);
        } catch (GloboxApplicationException e) {
            log.error("媒体视频上传失败: {}", e.getMessage());
            return R.error(e);
        } catch (Exception e) {
            log.error("媒体视频上传异常", e);
            return R.<VideoUploadVo>error(UserAuthCode.UPLOAD_FILE_FAILED).message("视频上传失败");
        }
    }
}