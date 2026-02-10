package com.unlimited.sports.globox.model.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 用户同步批量消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSyncBatchMessage implements Serializable {

    private List<UserSyncVo> users;
}
