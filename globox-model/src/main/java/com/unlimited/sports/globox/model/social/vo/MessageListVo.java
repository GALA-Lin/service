package com.unlimited.sports.globox.model.social.vo;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageListVo {

    /**
     * 消息列表
     */
    private List<MessageVo> messageVoList;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码
     */
    private int page;

    /**
     * 每页大小
     */
    private int pageSize;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 接收方的名字
     */
    @NonNull
    private String name;
    /**
     * 接收方的头像
     */
    @NonNull
    private String avatar;

}
