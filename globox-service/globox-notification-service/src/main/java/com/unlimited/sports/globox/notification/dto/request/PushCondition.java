package com.unlimited.sports.globox.notification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 推送条件
 * 说明: 支持标签条件和属性条件，建议不要混用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushCondition {

    /**
     * 标签条件的并集（最多10个）
     */
    @JsonProperty("TagsOr")
    private List<String> tagsOr;

    /**
     * 标签条件的交集（最多10个）
     */
    @JsonProperty("TagsAnd")
    private List<String> tagsAnd;

    /**
     * 标签条件的非集（最多10个）
     * 说明: 多个标签之间先取并集，再对该结果取补集
     */
    @JsonProperty("TagsNot")
    private List<String> tagsNot;

    /**
     * 属性条件的并集（最多10个）
     */
    @JsonProperty("AttrsOr")
    private Map<String, String> attrsOr;

    /**
     * 属性条件的交集（最多10个）
     */
    @JsonProperty("AttrsAnd")
    private Map<String, String> attrsAnd;
}
