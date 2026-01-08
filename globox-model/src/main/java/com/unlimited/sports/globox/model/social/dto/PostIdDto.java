package com.unlimited.sports.globox.model.social.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostIdDto {

    /**
     * 帖子ID
     */
    @NotNull
    private Long postId;
}
