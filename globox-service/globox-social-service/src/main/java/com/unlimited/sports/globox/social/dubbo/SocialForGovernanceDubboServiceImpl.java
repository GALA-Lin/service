package com.unlimited.sports.globox.social.dubbo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.dubbo.governance.dto.ContentSnapshotResultDto;
import com.unlimited.sports.globox.dubbo.social.SocialForGovernanceDubboService;
import com.unlimited.sports.globox.model.social.entity.*;
import com.unlimited.sports.globox.social.mapper.*;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 社交服务对治理服务提供的 dubbo 接口
 */
@Component
@DubboService(group = "rpc")
public class SocialForGovernanceDubboServiceImpl implements SocialForGovernanceDubboService {

    @Autowired
    private SocialNoteMapper socialNoteMapper;

    @Autowired
    private SocialNoteMediaMapper socialNoteMediaMapper;

    @Autowired
    private SocialNoteCommentMapper socialNoteCommentMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ConversationMapper conversationMapper;
    @Autowired
    private JsonUtils jsonUtils;

    @Override
    public ContentSnapshotResultDto getNoteSnapshot(Long id) {
        SocialNote socialNote = socialNoteMapper.selectById(id);
        List<SocialNoteMedia> socialNoteMediaList = socialNoteMediaMapper.selectList(
                Wrappers.<SocialNoteMedia>lambdaQuery()
                        .eq(SocialNoteMedia::getNoteId, id));

        List<String> mediaList = socialNoteMediaList.stream().map(SocialNoteMedia::getUrl).toList();

        return ContentSnapshotResultDto.builder()
                .id(id)
                .title(socialNote.getTitle())
                .content(socialNote.getContent())
                .mediaList(mediaList)
                .build();
    }

    @Override
    public ContentSnapshotResultDto getNoteCommentSnapshot(Long id) {
        SocialNoteComment socialNoteComment = socialNoteCommentMapper.selectById(id);

        return ContentSnapshotResultDto.builder()
                .id(id)
                .content(socialNoteComment.getContent())
                .build();
    }

    @Override
    public ContentSnapshotResultDto getIMMessageSnapshot(Long id) {
        Conversation conversation = conversationMapper.selectById(id);
        List<MessageEntity> messageEntities = messageMapper.selectList(
                Wrappers.<MessageEntity>lambdaQuery()
                        .eq(MessageEntity::getConversationId, conversation)
                        .orderByDesc(MessageEntity::getSendTime)
                        .last(" LIMIT 20"));
        return ContentSnapshotResultDto.builder()
                .id(id)
                .content(jsonUtils.objectToJson(messageEntities))
                .build();
    }
}
