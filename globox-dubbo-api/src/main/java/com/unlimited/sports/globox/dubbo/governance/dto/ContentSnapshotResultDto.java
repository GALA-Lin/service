package com.unlimited.sports.globox.dubbo.governance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 被举报内容快照
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentSnapshotResultDto implements Serializable {
    private Long id;
    private String title;
    private String content;
    private List<String> mediaList;
}
