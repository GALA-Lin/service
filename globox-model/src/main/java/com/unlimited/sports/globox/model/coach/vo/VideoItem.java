package com.unlimited.sports.globox.model.coach.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class VideoItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String url;       // 视频链接
    private String description; // 文字说明
}
