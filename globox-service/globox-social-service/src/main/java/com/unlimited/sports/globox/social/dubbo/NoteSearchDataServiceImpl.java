package com.unlimited.sports.globox.social.dubbo;

import com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.StringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.dubbo.social.INoteSearchDataService;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.vo.NoteSyncVo;
import com.unlimited.sports.globox.social.mapper.SocialNoteMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 笔记搜索数据RPC服务实现
 * 供搜索服务调用，获取笔记数据用于增量同步到Elasticsearch
 */
@Component
@Slf4j
@DubboService(group = "rpc")
public class NoteSearchDataServiceImpl implements INoteSearchDataService {

    @Autowired
    private SocialNoteMapper noteMapper;

    /**
     * 增量同步笔记数据
     *
     * @param updatedTime 上一次同步的时间戳，为null表示全量同步，不为null表示增量同步
     * @return 笔记同步数据列表（NoteSyncVO格式）
     */
    @Override
    public RpcResult<List<NoteSyncVo>> syncNoteData(LocalDateTime updatedTime) {
        try {
            log.info("开始同步笔记数据: updatedTime={}", updatedTime);

            // 不过滤状态
            LambdaQueryWrapper<SocialNote> wrapper = new LambdaQueryWrapper<>();
            if (updatedTime != null) {
                // 增量同步：查询 updated_at > updatedTime 的数据
                wrapper.gt(SocialNote::getUpdatedAt, updatedTime);
            }
            List<SocialNote> notes = noteMapper.selectList(wrapper);

            if (notes == null || notes.isEmpty()) {
                log.info("没有需要同步的笔记数据");
                return RpcResult.ok(List.of());
            }

            log.info("查询到笔记数据: 数量={}", notes.size());

            //  转换为NoteSyncVO
            List<NoteSyncVo> syncVOs = notes.stream()
                    .map(this::convertNoteToSyncVO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return RpcResult.ok(syncVOs);

        } catch (Exception e) {
            log.error("同步笔记数据异常: updatedTime={}", updatedTime, e);
            return RpcResult.error(SocialCode.NOTE_SYNC_FAILED);
        }
    }

    /**
     * 将SocialNote转换为NoteSyncVO
     */
    private NoteSyncVo convertNoteToSyncVO(SocialNote note) {
        try {
            if (note == null || note.getNoteId() == null) {
                return null;
            }

            // 解析标签（从JSON字符串转为List）
            List<String> tags = null;
            if (!StringUtil.isNullOrEmpty(note.getTags())) {
                try {
                    tags = List.of(note.getTags().split(";"));
                } catch (Exception e) {
                    log.error("解析笔记标签失败: noteId={}, tags={}", note.getNoteId(), note.getTags(), e);
                    tags = List.of();
                }
            }

            return NoteSyncVo.builder()
                    .noteId(note.getNoteId())
                    .userId(note.getUserId())
                    .title(note.getTitle())
                    .content(note.getContent())
                    .tags(tags != null ? tags : List.of())
                    .coverUrl(note.getCoverUrl())
                    .mediaType(note.getMediaType() != null ? note.getMediaType().name() : null)
                    .likeCount(note.getLikeCount() != null ? note.getLikeCount() : 0)
                    .commentCount(note.getCommentCount() != null ? note.getCommentCount() : 0)
                    .collectCount(note.getCollectCount() != null ? note.getCollectCount() : 0)
                    .status(note.getStatus())
                    .createdAt(note.getCreatedAt())
                    .updatedAt(note.getUpdatedAt())
                    .build();

        } catch (Exception e) {
            log.error("转换SocialNote为SyncVO失败: noteId={}", note.getNoteId(), e);
            return null;
        }
    }

}
