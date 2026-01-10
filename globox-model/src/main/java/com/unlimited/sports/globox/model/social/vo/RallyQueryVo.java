package com.unlimited.sports.globox.model.social.vo;

import com.unlimited.sports.globox.common.result.PaginationResult;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RallyQueryVo {

    @NonNull
    private List<String> area;
    @NonNull
    private List<String> timeRange;
    @NonNull
    private List<String> genderLimit;
    @NonNull
    private List<String> activityType;

    private PaginationResult<RallyPostsVo> rallyPostsVoList;

}
