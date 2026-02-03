package com.unlimited.sports.globox.social.dubbo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.dubbo.governance.dto.ContentSnapshotResultDto;
import com.unlimited.sports.globox.dubbo.social.SocialForGovernanceDubboService;
import com.unlimited.sports.globox.model.social.entity.MessageEntity;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.entity.SocialNoteComment;
import com.unlimited.sports.globox.model.social.entity.SocialNoteMedia;
import com.unlimited.sports.globox.social.mapper.MessageMapper;
import com.unlimited.sports.globox.social.mapper.SocialNoteCommentMapper;
import com.unlimited.sports.globox.social.mapper.SocialNoteMapper;
import com.unlimited.sports.globox.social.mapper.SocialNoteMediaMapper;
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
        MessageEntity messageEntity = messageMapper.selectById(id);

        return ContentSnapshotResultDto.builder()
                .id(id)
                .content(messageEntity.getContent())
                .build();
    }
}
