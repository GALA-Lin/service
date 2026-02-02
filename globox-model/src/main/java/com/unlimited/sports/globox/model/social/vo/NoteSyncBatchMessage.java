package com.unlimited.sports.globox.model.social.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 笔记同步批量消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteSyncBatchMessage implements Serializable {

    private List<NoteSyncVo> notes;
}
