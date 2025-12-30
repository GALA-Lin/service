package com.unlimited.sports.globox.model.venue.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 文件上传响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadVo {

    /**
     * 文件URL - 必须有
     */
    @NonNull
    private String url;

    /**
     * 文件名 - 前端需要显示文件名
     */
      
    private String fileName;

    /**
     * 文件大小（字节） - 前端需要显示文件大小
     */
      
    private Long fileSize;
}
