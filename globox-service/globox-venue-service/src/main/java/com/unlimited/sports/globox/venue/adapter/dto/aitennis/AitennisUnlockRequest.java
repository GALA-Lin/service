package com.unlimited.sports.globox.venue.adapter.dto.aitennis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * aitennis解锁请求
 */
@Data
@Builder
public class AitennisUnlockRequest {

    /**
     * 是否全部解锁
     */
    @JsonProperty("is_all")
    private Boolean isAll;

    /**
     * 要解锁的项目ID列表
     */
    private List<String> item;
}
