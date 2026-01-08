package com.unlimited.sports.globox.model.social.dto;

import com.unlimited.sports.globox.model.social.entity.MessageTypeEnum;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 消息传输对象
 */
@Data
public class MessageDto implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 发送方用户ID
     */
    @NotNull
    private Long fromUserId;

    /**
     * 接收方用户ID
     */
    @NotNull
    private Long toUserId;

    /**
     * 消息类型: 1-文本, 2-图像, 3-音频, 4-视频, 5-文件, 6-地址
     */
    @NotNull
    private MessageTypeEnum messageType;

    /**
     * 消息内容
     */
    @NotEmpty
    private String content;

    /**
     * 消息随机值(用于去重)
     */
    private Long random;

    /**
     * 扩展信息(JSON格式)
     */
    private String extra;
}
